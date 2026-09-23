# Rhythm - Habit Tracker — Architecture Map

Generated 2026-07-01 via a whole-codebase research pass (5 parallel research agents + direct knowledge from widget work earlier in the same session). This is a snapshot — code moves faster than docs, so treat file:line references as approximate and verify against the current source before relying on them for anything load-bearing.

App: Kotlin, Jetpack Compose, Room, Hilt DI, single-module (`app/`), package `com.apagon.rhythm`.

## How a typical feature is wired

Every feature in this app follows the same shape: a Compose screen collects state from a `@HiltViewModel`, the ViewModel exposes `StateFlow`s built by combining one or more `Flow`s from a `@Singleton` repository, and the repository wraps exactly one Room DAO (with `CalendarIntegrationRepository` as the sole exception — it talks to Android's `CalendarContract` content provider directly, no DAO). Writes are `suspend fun`s on the repository; almost every entity uses a **soft-delete pattern** (`deletedAt: Long?` field — `deleteX()` sets it, `hardDeleteX()` actually removes the row, `purgeOldDeletedItems(cutoff)` sweeps old soft-deleted rows). Time-based features (alarms/timers/reminders) additionally schedule an `AlarmManager` entry that fires a `BroadcastReceiver`, which posts a notification and can launch a full-screen alert `Activity`. Widgets read the same repositories directly (via a Hilt `EntryPoint`, since Glance widgets aren't part of the normal DI graph) and must be explicitly told to refresh — there's no automatic Flow-to-widget bridge.

---

## Data Foundation

**Models** (`data/model/`, 13 files, all Room `@Entity`): `Habit`, `HabitCompletion`, `ChecklistItem`/`ChecklistItemCompletion` (+ non-entity `ChecklistProgress` DTO), `Alarm`, `Reminder`, `Timer` (conflates plain countdown + full Pomodoro state machine in one table), `Note`, `Notebook`, `JournalEntry`, `Todo`, `CalendarEvent` (dual-purpose — see Content Features).

**Repositories** (`data/repository/`, 10 files): all `@Singleton`, one DAO each, Flow-based reactive getters + suspend writers + the soft-delete pattern above. `HabitRepository` additionally owns checklist-item CRUD/completion with a re-query-mid-transaction guard against rapid-tap races. `CalendarIntegrationRepository` is the odd one out (no DAO, talks to `CalendarContract` directly, encodes native-calendar events with **negative IDs** so the rest of the app can tell them apart from app-owned rows sharing the same `CalendarEvent` class).

**DAOs & Database** (`data/db/`): 8 DAOs, `HabitDatabase` currently **version 33**, one `TypeConverter` (`HabitFrequencyConverter`). Migration chain 1→33, all manual, registered in `DatabaseModule`. Notable migrations: `MIGRATION_8_9` (converted all MONTHLY habits to DAILY — MONTHLY was later reinstated, so pre-v9 upgraders may have stale data), `MIGRATION_22_23` (bulk-added `deletedAt` to nine tables — this is when soft-delete was retrofitted app-wide), `MIGRATION_32_33` (remapped `Alarm.repeatDaysMask` bit order Monday→Sunday-based via SQL). `fallbackToDestructiveMigrationOnDowngrade()` is set; there's no forward-gap fallback, so a missing migration crashes rather than silently degrading.

**DI** (`di/`): `DatabaseModule` (the Room builder + all migrations + one `@Provides` per DAO) and `BillingModule` (provides `BillingRepository`) — the only two Hilt modules in the app.

**Preferences** (`data/preferences/ThemePreferences.kt`): one DataStore (`"settings"`) doing far more than theming — theme mode/AMOLED/accent color/contrast level, user profile fields, `isPro`, onboarding/tutorial flags, calendar-integration settings, and JSON export/import for backup. Name no longer matches scope.

**Security** (`data/repository/SecurityRepository.kt`): **not** app-wide encryption — it's a **Journal-specific** access gate. Separate DataStore (`"security_prefs"`) holding `LockType` (NONE/PIN/PASSWORD, mutually exclusive), the PIN/password, and a biometric-shortcut flag. Consumed only by `ui/journal/JournalLockScreen.kt`. **PIN/password are stored as plaintext strings** — no hashing, no Keystore/EncryptedSharedPreferences.

---

## Core Habit Loop

**Home screen** (`ui/habit/HabitListScreen.kt` + `HabitListViewModel.kt`): two mutually-exclusive layouts (today/list vs. calendar-month), switchable in Settings. `HabitsUiState` (grouped/pending/completed habits + completions + checklist progress) is built via a `flatMapLatest` on selected date combining habits, checklist items, and completion Flows.

**Detail/editing**: `HabitDetailSheet.kt` (view + "Complete Habit" button, streak shown), `AddHabitSheet.kt` (one composable for both add and edit — name, description, color/icon, frequency with weekday/month-day bitmasks, goal duration, reminder, checklist toggle).

**Completion flow**: simple habits toggle optimistically in `HabitRowComposable.kt` then call `HabitListViewModel.toggleCompletion()` → `repository.markComplete/markIncomplete` → `refreshAllWidgets(context)`. Checklist habits toggle per-item via `toggleItemCompletion()` → `repository.toggleChecklistItem()`, which auto-completes/uncompletes the parent habit. **Asymmetry to know about:** a checklist habit's row can only be tapped to mark *incomplete* — marking it complete requires checking all subtasks or using the detail sheet's button.

**Streaks**: computed in the ViewModel (`dailyStreak`, `getStreakForHabit`), not the repository — walks a sorted set of completion dates backward from today.

**Scheduling**: `Habit.isScheduledForDate()` (`ui/util/Extensions.kt`) is bitmask-driven — DAILY always true, WEEKLY checks `weekDaysMask`, MONTHLY checks `monthDaysMask`. Habits scheduled for the 31st simply don't fire in shorter months (bitmask has no "last day" concept).

**Stats/Trends**: `StatsViewModel.kt` builds a full calendar-year `HabitYearStats` per habit. **The `StatsScreen()` composable that the nav tab actually renders just delegates entirely to `JournalScreen()`** — the real progress UI in that same file (`HabitProgressCard`, `StatsHabitSheet`) is only reachable via `YourProgressSheet` invoked from the habit list, not from the Stats tab itself. Needs a product-intent check, not an assumed bug.

`HabitListViewModel` has grown into a de facto God-ViewModel — it also owns reminders, todos, calendar events, and todo-archival/purge logic, well beyond "habit" scope.

---

## Time-Based Features (Alarms / Timers / Reminders)

**Alarms** (`ui/alarms/AlarmViewModel.kt`): thin wrapper over `AlarmRepository`; writes go through `ReminderScheduler.scheduleAlarm()`, which computes the next trigger (7-day lookahead, fires today if today qualifies and the time hasn't passed) and uses `AlarmManager.setAlarmClock()` — immune to Doze, shows the system alarm-clock icon — falling back to `setAndAllowWhileIdle` only if exact-alarm permission is missing (API 31+).

**Timers** (`ui/alarms/TimerViewModel.kt`): structurally different — does **not** go through `ReminderScheduler`. Schedules its own `AlarmManager.setExactAndAllowWhileIdle` targeting `TimerCompletionReceiver`, and starts `TimerForegroundService` for the persistent countdown notification (`setUsesChronometer` instead of a redraw loop, specifically for Samsung Now Bar compatibility — see `feedback_samsung_nowbar_limits` memory). Pomodoro is modeled as extra fields on the same `Timer` entity; phase transitions re-arm a fresh alarm and restart the foreground service.

**One-shot reminders** (`ui/reminders/ReminderViewModel.kt`): always `setExactAndAllowWhileIdle`, fire once, never rescheduled — except habit reminders (`TYPE_HABIT`), which explicitly re-arm the next day's occurrence after firing. Reminders/Todos/Habits share `GenericAlertActivity`; Alarms get a dedicated `AlarmAlertActivity`; Timers get `TimerAlertActivity`.

**Notification channels**: 7 static channels created once in `HabitApp.createNotificationChannels()` (habit reminders, silent-by-design alarms/timers channels — sound comes from each alert Activity's own `MediaPlayer` — a silent-high-importance channel for heads-up-without-sound on newer Android, a default-sound alerts channel, a silent-low channel, and a silent ongoing-timer channel required for Samsung Now Bar). On top of these, both `ReminderReceiver` and `TimerCompletionReceiver` mint **dynamic per-sound-URI channels at runtime**, keyed by a hash of sound+vibration pattern, since Android forbids changing a channel's sound after creation.

**End-to-end flow**: UI → ViewModel → Room write → scheduler computes trigger → `PendingIntent` → `AlarmManager` → receiver fires → resolves a channel → posts `NotificationCompat` with a `setFullScreenIntent` (gated by `canUseFullScreenIntent()`, API 34+) → alert Activity wakes the screen, plays its own looping `MediaPlayer`, vibrates via `AlertVibrator`. `BootReceiver` re-arms everything on `ACTION_BOOT_COMPLETED` since scheduled alarms don't survive reboot.

**Real risk flagged, not yet confirmed by compiling**: `notifications/NotificationConstants.kt` declares top-level `EXTRA_SOUND_URI = "soundUri"` etc., while `ReminderReceiver`'s own companion object appears to independently redeclare similar-looking constants with different string values (e.g. `"sound_uri"`). Kotlin resolves unqualified names to a class's own companion before falling back to package-level declarations — if that's really happening here, a custom alarm sound could silently fail to round-trip through certain call sites. **Needs a dedicated verification pass** (grep every `EXTRA_*` definition and every read/write site) before treating as confirmed.

Other flagged duplication: `ensureDynamicChannel`/`ensureSilentDynChannel`/`ensureDefaultSoundDynChannel`/`startLoopingVibration`/`getVibrator`/`canUseFullScreenIntent` are copy-pasted verbatim between `ReminderReceiver.kt` and `TimerCompletionReceiver.kt`; `resolveChannel`/`resolveAlarmChannel` inside `ReminderReceiver` itself are identical bodies. Vibration appears to start twice per alert (once from the receiver, once from the alert Activity's `onCreate`) — may be intentional redundancy, may be a bug.

---

## Content Features (Notes / Journal / Todos / Calendar)

Four features that share the same repository/soft-delete shape but otherwise vary widely in how (or whether) they connect to Habits.

**Notes** (`ui/notes/`, 12 files) — **no relationship to Habits**. Two-level hierarchy: `Notebook` → `Note`, where `Note.content` is a hand-serialized JSON array of blocks. `NoteBlock`/`BlockType` (TEXT/HEADER/CHECKLIST/BULLET_LIST/NUMBERED_LIST/QUOTE/CODE/DIVIDER/IMAGE) are defined inside `NoteEditorViewModel.kt` rather than `data/model/` — unusual for a DB-persisted shape. Serialization is manual `org.json` parsing, not a `TypeConverter` or kotlinx.serialization — fragile if fields are added later (silent defaulting via `optString`/`optBoolean`, not compile-checked). Templates (`NoteTemplates.kt`) are a `sealed class` with a `buildBlocks()` extension; the "Daily Journal" template was removed earlier this session in favor of the dedicated Journal feature.

**Journal** (`ui/journal/`, 6 files) — **optional integration with Habits**. `JournalEntry.habitId` is nullable: `null` = general daily entry, non-null = a per-habit note for that date. `JournalViewModel.habitNotes` pairs every active habit with its optional entry for the selected date. Also has mood (0-5), a curated feelings chip set, photo attachments, PIN/password/biometric lock (via `SecurityRepository`, see above), and a Pro paywall capping non-Pro users at 3 entries/day (checked inline in `saveEntry`, not centralized). **Flagged inconsistency**: the tutorial overlay tells users Journal will help them "build streaks," but no streak field or calculation exists anywhere in the Journal code — dead copy or an unbuilt feature, needs a product call before touching either way.

**Todos** (`ui/todos/`, 3 files) — **no data relationship to Habits** (one composable imports `HabitCard` for shared visual styling only). Simple priority (`HIGH/MEDIUM/LOW/NONE`) + due-date model, no sub-checklist. `refreshAllWidgets()` called after add/toggle/delete.

**Calendar** (`ui/calendar/`, 3 files) — **the real cross-cutting hub**, unlike the other three. `CalendarViewModel` combines habit scheduling/completion, reminders, in-app `CalendarEvent`s, and (Pro-gated, toggleable) native device-calendar events into one month view. Native vs. local events are distinguished by a **negative-ID sentinel** (`event.id < 0` → native provider call) rather than an explicit source field — a working but implicit contract that a new code path could easily violate by not checking the sign first.

---

## Supporting Flows, Monetization & Theming

**Onboarding & Tutorial**: 3-page onboarding (`ui/onboarding/OnboardingScreen.kt`) gated by `ThemePreferences.hasSeenOnboarding`; on completion, seeds a sample habit and immediately kicks off an interactive tutorial (`TutorialState`/`TutorialOverlay` in `ui/tutorial/`) — a step machine (`NONE → HABITS → CALENDAR_LIST → MULTI_HABIT → JOURNAL → REMINDERS → SETTINGS → NONE`) drawing a scrim-with-cutout around registered target rects. **3 of 6 steps (MULTI_HABIT, REMINDERS, SETTINGS) have no tooltip copy defined** — they render a blank tooltip body. Settings has a "Restart Tutorial" action.

**Deleted-item recovery** (`ui/deleted/`): one screen sectioned by type across 9 entity kinds, all using the same nullable `deletedAt` soft-delete field. `CleanupManager.purgeAll()` sweeps anything older than 14 days — but **only runs once, on cold app start** (not a recurring `WorkManager` job like widget refresh is), so the "permanently deleted after 14 days" promise in the UI can silently slip for users who don't force-quit/relaunch often.

**Settings** (`ui/settings/`, 7 files, ~1400-line `SettingsScreen.kt`): profile, permissions status, calendar integration, accent color (Pro-gated), backup/restore, archive sheets, theme mode + "Night Readability" contrast levels, home-view mode, notification sound deep-link, Samsung Now Bar info, About (version, restore purchases, restart tutorial, privacy policy). Two secret gestures on the version number (7-tap → passcode dialog → VIP promo billing flow or crash-log debug mode — both built this session). **File-naming mismatch**: `AccountSettings.kt`/`BehaviorSettings.kt`/`AppearanceSettings.kt` don't hold what their names suggest — `AppearanceSettings.kt` is a 3-line unimplemented stub; most real settings UI still lives monolithically in `SettingsScreen.kt`.

**Billing** (`data/billing/BillingRepository.kt`): wraps Play Billing (`BillingClient`), two subscription SKUs (`pro_monthly_subscription`/`pro_annual_subscription`), a VIP promo offer on the annual SKU unlocked via a **hardcoded client-side passcode** (`RHY-7K4P-VIP`) — trivially extractable from the APK by anyone who decompiles it. Auto-reconnects on disconnect (added this session).

**Theming** (`ui/theme/`): `RhythmTheme()` wraps materialkolor's `DynamicMaterialTheme` (seed color → generated tonal palette, `PaletteStyle.TonalSpot`), custom typography (Plus Jakarta Sans + Inter via Google Fonts provider) and custom corner-radius shape scale. `Color.kt`'s static M3-violet palette (`GCLight*`/`GCDark*` tokens) **appears to be dead code** now that theming is fully dynamic-seed-based — referenced nowhere outside its own file. **The AMOLED dark-mode toggle is fully wired end-to-end** (DataStore field, ViewModel getter/setter, `RhythmTheme(isAmoled=...)` parameter all exist and work) **but `SettingsScreen.kt` has no UI control that calls `setAmoledMode()`** — a functionally orphaned feature, invisible to users.

---

## Widgets

(Written from direct session knowledge — this subsystem was worked on extensively earlier in the same session, not re-derived by a fresh agent.)

5 Jetpack Glance home-screen widgets, all in `widget/` (19 files): **Habit Ring** (2×2, aggregate or single-habit completion ring), **Habit Grid** (4×2, up to 6 habit circles, tap to complete), **Today Glance** (4×2, habits/to-dos/next-reminder summary, sections independently toggleable), **Streak Spotlight** (2×2, drum-icon streak counter, 15 selectable drum icons), **Active Timer** (2×2, shows the one currently-running timer — intentionally has no config screen, since the app only ever has zero-or-one active timer at a time).

Each widget reads data via a Hilt `EntryPoint` (`WidgetEntryPoint`) since Glance widgets sit outside the normal Compose/DI tree, separates data-fetching (`provideGlance`) from rendering (a private `XContent(...)` composable taking plain parameters — this separation is why adding real, exact widget-picker previews via `providePreview()` was possible with zero new UI code). Four of the five have a config `Activity` (Hilt `@AndroidEntryPoint` `ComponentActivity`, state stored via `PreferencesGlanceStateDefinition`), reachable via long-press → Edit or at placement time. Widgets **do not auto-refresh on state changes** — every ViewModel action that should visibly affect a widget (timer start/pause/reset/delete, habit completion toggle, checklist item toggle, todo add/complete/delete) must explicitly call `refreshAllWidgets(context)` (`widget/WidgetUtils.kt`); this was incomplete/inconsistent before this session's fixes (only 2 of 5 widgets refreshed, only on habit-complete not habit-incomplete) and has since been made consistent across all of them. Tap targets deep-link into specific app tabs via `MainActivity.EXTRA_OPEN_TAB` (added this session — previously every widget just opened the app to whatever screen was last open). Widget-picker previews use Glance's `providePreview()` API (requires `glance-appwidget:1.2.0-rc01`, and publishing via `setWidgetPreviews()` is Android 15+ only — older devices fall back to static XML `previewLayout` mockups).
