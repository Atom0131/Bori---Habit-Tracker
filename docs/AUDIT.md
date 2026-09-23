# Rhythm - Habit Tracker — Bug / Tech-Debt Audit

Point-in-time snapshot, 2026-07-01. Produced by 5 targeted research agents, each verifying candidate issues surfaced while writing `docs/ARCHITECTURE.md`, then hunting for new issues of the same class. Every item below cites real file:line evidence — nothing here is a guess. Verdicts: **CONFIRMED** (traced with code evidence), **PLAUSIBLE** (strong evidence, not fully proven), **REFUTED** (flagged earlier, checked, not actually a problem). This is a punch list, not a set of applied fixes — nothing in this document has been changed.

---

## High severity

### 1. Habit reminders, one-shot reminders, todos, and events all lose their id/title/note/custom-sound in the full-screen alert — CONFIRMED
`ui/reminders/GenericAlertActivity.kt:70-74` reads extras via a wildcard import of the top-level `notifications/NotificationConstants.kt` (camelCase keys: `"habitId"`, `"reminderTitle"`, `"soundUri"`, etc.). But `ReminderReceiver.genericFullScreenIntent()` (`notifications/ReminderReceiver.kt:433-454`) writes those same-named extras unqualified — which resolves to `ReminderReceiver`'s own companion object (`ReminderReceiver.kt:534-571`, snake_case: `"habit_id"`, `"reminder_title"`, `"sound_uri"`, etc.). Every key mismatches except `EXTRA_VIBRATION_PATTERN_ID` (coincidentally identical both places) and the explicitly-qualified `EXTRA_NOTIF_ID`/`EXTRA_TYPE`.
**User-visible effect:** whenever a habit reminder, reminder, todo, or event alert wakes the screen, `GenericAlertActivity` shows `alertId = -1`, falls back to the generic title `"Reminder"` (real habit/reminder name lost), blank note/time, and the **default** ringtone instead of the user's chosen sound. Worse: because `alertId` is wrong, tapping Done/Snooze on the notification while the alert screen is open fails to close it (its stop-receiver is listening on the wrong id).

### 2. Alarms always play the default ringtone, never the user's custom alarm sound — CONFIRMED
Same root cause, narrower blast radius: `ui/alarms/AlarmAlertActivity.kt:43,84` reads `EXTRA_SOUND_URI` from the top-level constant (`"soundUri"`); `ReminderReceiver.alarmFullScreenIntent()` (`ReminderReceiver.kt:181-183,456-470`) writes it unqualified → resolves to the companion's `"sound_uri"`. Every other alarm extra happens to have identical values both places, so only the custom sound is lost — the alert always falls back to `RingtoneManager.getDefaultUri`, and a 5-minute snooze re-schedules with the now-empty sound too.
**Fix shape (not applied):** both #1 and #2 share one root cause — pick one canonical set of `EXTRA_*` constants (the top-level `NotificationConstants.kt` ones, since they're already imported by the receiving Activities) and make `ReminderReceiver`'s intent-building functions reference them explicitly instead of relying on unqualified name resolution inside the class body. This is likely a single, well-contained fix once scoped properly.

### 3. Deleting a habit doesn't cancel its scheduled reminder — it keeps firing forever — CONFIRMED
`HabitListViewModel.kt:725-727` `deleteHabit()` only calls `repository.deleteHabit(habit)` (soft-delete) — unlike the edit-habit path, which does call `ReminderScheduler.cancelReminder()`. Compounding this, `HabitDao.getHabitById` (`HabitDao.kt:32`) has no `deletedAt IS NULL` filter, so `ReminderReceiver.kt:313`'s post-fire re-arm logic (`habitRepository.getHabitById(habitId).first(); if (habit != null) scheduleReminder(...)`) still finds the "deleted" habit and keeps re-arming it.
**User-visible effect:** delete a habit that has a daily reminder → the reminder keeps notifying indefinitely (well past any 14-day purge window, since the outstanding `AlarmManager` entry is never cancelled), showing the name of a habit the user believes they removed.

### 4. Interactive tutorial silently ends after the Journal step — REMINDERS and SETTINGS are unreachable for everyone — CONFIRMED
`ui/tutorial/TutorialOverlay.kt:108-113`: the "Next Step" button is hardcoded `if (step == JOURNAL) onFinish() else tutorialState.next()`. Step order is `...MULTI_HABIT → JOURNAL → REMINDERS → SETTINGS`, so reaching Journal and tapping Next always ends the tutorial instead of advancing. Nothing else in the codebase ever calls `.next()` from the REMINDERS/SETTINGS steps. This is worse than "blank tooltip copy" (the original suspicion) — it's genuinely dead, unreachable state for every user going through the tutorial normally. Separately, MULTI_HABIT (which *is* reachable) falls into an `else -> "" to ""` branch and renders a visibly half-empty tooltip card with a live Next button.

### 5. Default navigation mode recreates the entire habit-list ViewModel (and reruns a full DB scan) on every tab switch, with an unbounded back stack — CONFIRMED
`MainActivity.kt:428-440`'s `StandardNavigationContent` (used whenever `swipeSectionsEnabled` is false — the **default**, `ThemePreferences.kt:75`) calls `navController.navigate(tab.route) { launchSingleTop = true }` with no `popUpTo`/`saveState`/`restoreState`. Every tab switch pushes a new back-stack entry, so `hiltViewModel()` in `HabitListScreen.kt:168` gets tied to a fresh entry each time — `HabitListViewModel` (and its `init { archiveOverdueTodos() }`, a full todo table scan-and-write) gets fully recreated every time a user returns to Home. System Back also cycles through tab history instead of exiting the app. The pager-based `SwipeNavigationContent` variant doesn't have this problem — only the default mode does.

### 6. The "Stats" nav tab shows the Journal screen, not stats — CONFIRMED (verified directly)
`ui/stats/StatsScreen.kt:159-161` is a literal one-line passthrough: `fun StatsScreen(onNavigateToSettings) { JournalScreen(onNavigateToSettings) }`. Every other composable in that same file — `HabitProgressCard`, `StatsHabitSheet`, the year-progress calculation logic — is real, functional stats UI, but it's only reachable via `YourProgressSheet` invoked from the habit list screen, never from the nav tab labeled "Stats" itself. Whatever that tab is supposed to show, right now every user who taps it sees Journal. Either this is leftover from a mid-refactor swap (Stats and Journal got their nav destinations crossed), or the tab is intentionally aliased and mislabeled — worth a product decision either way, since the fix is a one-line change once the intent is confirmed.

### 7. Checklist "uncheck" path can leave a habit permanently stuck "complete" — PLAUSIBLE, well-evidenced
`HabitRepository.kt:140-150`: the *check* path was already fixed (per an earlier session) to re-query the DB directly and avoid rapid-tap staleness — but the *uncheck* path still trusts a caller-supplied snapshot (`HabitListViewModel.kt:539` passes `todayCompletions.value`; `CalendarViewModel.kt:313` passes a similar stale-prone snapshot). If a user checks the last item of a checklist habit (auto-completing it) and immediately unchecks a different item before the completions `StateFlow` catches up, `markIncomplete` gets silently skipped — the habit stays marked complete in the DB despite an unchecked subtask. Present at both call sites (habit list and calendar), not a one-off.

---

## Medium severity

### 8. Pre-v9 users with MONTHLY habits are permanently stuck on DAILY — CONFIRMED
`HabitDatabase.kt:128-133` (`MIGRATION_8_9`) converted all MONTHLY habits to DAILY when the frequency was removed; it was later reinstated, but no later migration backfills `monthDaysMask` or restores frequency for habits that went through that one-time conversion.

### 9. `getHabitById`/`TimerDao.getById` can resurrect soft-deleted rows — CONFIRMED
`HabitDao.kt:32` and `TimerDao.kt:23` are missing the `deletedAt IS NULL` filter that every sibling list query in the same DAO applies. Timers are safe in practice today because `TimerViewModel.deleteTimer` correctly cancels the alarm before deleting — but the DAO-level gap is the same class of bug as #3, one call-site slip away from recurring.

### 10. AMOLED dark-mode toggle is fully wired but has zero UI control — CONFIRMED
`SettingsViewModel`/`ThemePreferences`/`MainActivity`'s `RhythmTheme(isAmoled=...)` all work end-to-end; an exhaustive search of all `ui/settings/` files finds no Switch/row that ever calls `setAmoledMode()`. Shipped, functional, and completely inaccessible to users.

### 11. `CleanupManager.purgeAll()` never runs on a recurring schedule — CONFIRMED
Only called once, from `HabitApp.onCreate()`. No `WorkManager` job exists for it (unlike `WidgetUpdateWorker`, which runs every 15 minutes). The "permanently deleted after 14 days" promise shown in Recently Deleted can slip indefinitely for users who don't relaunch the app often.

### 12. Checklist habit row can only be tapped to mark *incomplete*, never complete — CONFIRMED, looks unintentional
`HabitRowComposable.kt:102`: `if (!habit.isChecklist || localIsDone) { onToggle() }` — tapping a not-yet-done checklist habit's row silently does nothing, no visual affordance explaining why. No comment justifies this; reads like the simple-habit toggle logic wasn't gated out for checklist habits rather than a deliberate design choice.

### 13. Journal PIN/password stored as plaintext, no hashing — CONFIRMED
`SecurityRepository.kt` stores the Journal lock PIN/password as raw strings in DataStore — no hashing/salting, no Keystore/EncryptedSharedPreferences. Readable directly by anyone with root or backup-extraction access to the device.

### 14. Dead per-habit-journaling code has no Pro-paywall guard — CONFIRMED (latent risk)
`JournalViewModel.kt:165-232` — `dailyEntry`, `habitNotes`, `saveGeneralEntry()`, `saveHabitNote()` are defined but never called from any screen (superseded by the `habitId` param on the live `saveEntry()`). None of them have the 3-entries/day Pro cap that `saveEntry()` enforces. Not exploitable today since nothing calls them — but a real full paywall bypass if anyone re-wires them without re-adding the guard.

### 15. VIP promo passcode is a hardcoded client-side string — CONFIRMED (carried from architecture pass, not separately re-audited)
`BillingRepository.VIP_PROMO_PASSCODE = "RHY-7K4P-VIP"` — trivially extractable via APK decompilation by anyone, independent of any real promo channel.

### 16. `queryPurchases()` on cold start is a near-guaranteed no-op; no resume-based re-check — CONFIRMED (staleness, not a bypass)
`MainActivity.kt:108` calls `queryPurchases()` in `onCreate`, but `BillingRepository.kt:80-81` bails immediately if the billing client isn't ready yet — and `startConnection()` is async, so it almost never is at that exact call site. Real sync only happens via `onBillingSetupFinished`'s callback. No `onResume`/`onStart` re-check exists anywhere. A user who buys Pro on one device and opens the app on another without force-restarting may see the paywall until they manually tap "Restore Purchases." No entitlement is ever granted without a real purchase or the VIP passcode flow — this is a UX/staleness issue, not a security hole.

---

## Low severity / cosmetic

- **`NotesDao.getNotebookById`/`getNoteById` missing `deletedAt` filter** (same class as #9, lower risk — current call sites are reached only via already-filtered active lists).
- **`CalendarIntegrationRepository`'s 6 broad `catch (e: Exception) { e.printStackTrace() }` blocks** swallow all content-provider errors, making a real sync failure indistinguishable from "no native events exist" — no user-visible signal either way.
- **Vibration fires twice per alert** (once from the receiver posting the notification, once from the alert Activity's `onCreate`) — confirmed redundant, but since `EXTRA_VIBRATION_PATTERN_ID` is one of the extras that transmits correctly, the second call just restarts the identical waveform from tick 0 — a brief, likely-imperceptible hiccup, not a doubled pattern.
- **Heavy code duplication** between `ReminderReceiver.kt` and `TimerCompletionReceiver.kt` (`ensureDynamicChannel`, `ensureSilentDynChannel`, `ensureDefaultSoundDynChannel`, `startLoopingVibration`, `getVibrator`, `canUseFullScreenIntent` all copy-pasted verbatim; `resolveChannel`/`resolveAlarmChannel` inside `ReminderReceiver` itself are identical bodies) — refactor candidate, not a bug.
- **Dead code, safe to remove:** `Color.kt`'s entire `GCLight*`/`GCDark*` static palette (100% unreferenced anywhere), `OnboardingScreen.kt`'s `ComposeColor()` function and `OnboardingFeatureItem` composable (both unused), `TimerViewModel.TIMER_NOTIF_ID_BASE` (unused, though currently in sync with its two live duplicates in `TimerAlertActivity`/`TimerCompletionReceiver` — a landmine if one copy is ever edited alone without the others).
- **Manual JSON note-block serialization has no version field**, but degrades gracefully — an unrecognized future `BlockType` silently flattens to plain `TEXT` (content preserved, type-specific rendering lost), not a crash or data loss.
- **Journal's Pro paywall counts entries for the currently *viewed* date, not necessarily today**, and only guards new entries (editing existing ones is uncapped) — likely intentional, just worth knowing.

## Checked, no issue found (flags cleared during audit)

- **`CalendarEvent.id < 0` native/local sentinel** — traced every write path (`CalendarViewModel`, `HabitListViewModel`, `CalendarIntegrationRepository`, `AddCalendarEventSheet`, `BackupManager`); all correctly branch on sign, no cross-contamination found.
- **`todayCompletions` vs `todayCompletionsFixed`** — misleading names, but every call site uses the correct one for its purpose; not a live bug, just worth renaming (`todayCompletions` → `selectedDateCompletions`) for clarity.
- **`NoteBlock`/`BlockType` defined inside `NoteEditorViewModel.kt`** — cosmetic only; no duplicate enum exists elsewhere, no consumer needs an extra import (same package).
- **Migration declarations out of numeric order in `HabitDatabase.kt`** — purely cosmetic; Room indexes by version pair via `addMigrations(...)`, not file order, and all 32 migrations are present.
- **`JournalEntry.habitId` SET_NULL vs. CASCADE elsewhere** — no code path assumes cascade; orphaned entries safely display as general entries after a habit is hard-deleted.
