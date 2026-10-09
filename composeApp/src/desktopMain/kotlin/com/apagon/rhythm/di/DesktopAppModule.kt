package com.apagon.rhythm.di

import com.apagon.rhythm.data.backup.BackupManaging
import com.apagon.rhythm.data.backup.DesktopBackupManager
import com.apagon.rhythm.data.db.DesktopHabitDatabase
import com.apagon.rhythm.data.db.buildDesktopHabitDatabase
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.preferences.buildDesktopSecurityDataStore
import com.apagon.rhythm.data.preferences.buildDesktopThemeDataStore
import com.apagon.rhythm.data.repository.AlarmRepository
import com.apagon.rhythm.data.repository.CalendarEventRepository
import com.apagon.rhythm.data.repository.DesktopDeviceCalendarIntegration
import com.apagon.rhythm.data.repository.DeviceCalendarIntegration
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.JournalRepository
import com.apagon.rhythm.data.repository.NotesRepository
import com.apagon.rhythm.data.repository.ReminderRepository
import com.apagon.rhythm.data.repository.SecurityRepository
import com.apagon.rhythm.data.repository.TimerRepository
import com.apagon.rhythm.data.repository.TodoRepository
import com.apagon.rhythm.data.sync.DEFAULT_SYNC_PORT
import com.apagon.rhythm.data.sync.DesktopSyncPreferences
import com.apagon.rhythm.data.sync.JvmSyncCoordinator
import com.apagon.rhythm.data.sync.LocalSyncAddress
import com.apagon.rhythm.data.sync.SyncClient
import com.apagon.rhythm.data.sync.SyncCoordinator
import com.apagon.rhythm.data.sync.SyncEngine
import com.apagon.rhythm.data.sync.SyncPreferences
import com.apagon.rhythm.data.sync.SyncServer
import com.apagon.rhythm.data.sync.findTailscaleAddress
import com.apagon.rhythm.platform.AlertCenter
import com.apagon.rhythm.platform.DesktopAlarmClockService
import com.apagon.rhythm.platform.DesktopOsNotifier
import com.apagon.rhythm.platform.DesktopHapticAlerter
import com.apagon.rhythm.platform.DesktopImageBitmapLoader
import com.apagon.rhythm.platform.DesktopLocaleFormatting
import com.apagon.rhythm.platform.DesktopPhotoStorage
import com.apagon.rhythm.platform.DesktopPurchaseLauncher
import com.apagon.rhythm.platform.DesktopQrCodeRenderer
import com.apagon.rhythm.platform.DesktopReminderScheduling
import com.apagon.rhythm.platform.DesktopFilePickerService
import com.apagon.rhythm.platform.DesktopVaultFileSync
import com.apagon.rhythm.platform.DesktopWidgetRefresher
import com.apagon.rhythm.platform.FilePicker
import com.apagon.rhythm.platform.HapticAlerter
import com.apagon.rhythm.platform.ImageBitmapLoader
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.platform.PhotoStorage
import com.apagon.rhythm.platform.PurchaseLauncher
import com.apagon.rhythm.platform.QrCodeRenderer
import com.apagon.rhythm.platform.ReminderScheduling
import com.apagon.rhythm.platform.VaultFileSync
import com.apagon.rhythm.platform.WidgetRefresher
import com.apagon.rhythm.ui.alarms.AlarmViewModel
import com.apagon.rhythm.ui.alarms.TimerViewModel
import com.apagon.rhythm.ui.calendar.DesktopCalendarViewModel
import com.apagon.rhythm.ui.deleted.RecentlyDeletedViewModel
import com.apagon.rhythm.ui.habit.DesktopHabitViewModel
import com.apagon.rhythm.ui.habit.HabitListViewModel
import com.apagon.rhythm.ui.journal.JournalViewModel
import com.apagon.rhythm.ui.notes.NoteEditorViewModel
import com.apagon.rhythm.ui.notes.NotebookDetailViewModel
import com.apagon.rhythm.ui.notes.NotesSearchViewModel
import com.apagon.rhythm.ui.notes.NotesViewModel
import com.apagon.rhythm.ui.reminders.ReminderViewModel
import com.apagon.rhythm.ui.settings.DesktopSettingsViewModel
import com.apagon.rhythm.ui.todos.TodoViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

// A fresh id per process launch — good enough for Stage 4b/4c's metadata
// (SyncBatch.deviceId isn't used for identity/dedup logic, only diagnostics),
// so it's deliberately not persisted.
@OptIn(ExperimentalUuidApi::class)
private val desktopDeviceId = "desktop-" + Uuid.random().toString().take(8)

// Mirrors androidMain/di/AppModule.kt's shape but scoped to just what's been
// ported to desktop so far: the Habit + Todo database/DAO/repositories,
// (Stage 5) real ThemePreferences persistence, and (Stage 6) the
// cross-cutting platform shims the rest of the full-port roadmap builds on.
// Notifications, real billing, real calendar sync, the Notes vault, Journal,
// Alarms, and Timers are still deliberately not wired here — see ref_notes/
// for the staged full-port roadmap.
val desktopAppModule = module {
    single<DesktopHabitDatabase> { buildDesktopHabitDatabase() }
    single { get<DesktopHabitDatabase>().habitDao() }
    single { get<DesktopHabitDatabase>().todoDao() }
    single { get<DesktopHabitDatabase>().calendarEventDao() }
    single { get<DesktopHabitDatabase>().journalDao() }
    single { get<DesktopHabitDatabase>().notesDao() }
    single { get<DesktopHabitDatabase>().alarmDao() }
    single { get<DesktopHabitDatabase>().reminderDao() }
    single { get<DesktopHabitDatabase>().timerDao() }
    single { get<DesktopHabitDatabase>().eventReminderDao() }
    single { get<DesktopHabitDatabase>().todoSubtaskDao() }
    single { get<DesktopHabitDatabase>().noteLinkDao() }
    single<WidgetRefresher> { DesktopWidgetRefresher() }
    single<SyncPreferences> { DesktopSyncPreferences() }
    single { ThemePreferences(buildDesktopThemeDataStore()) }
    single { SecurityRepository(buildDesktopSecurityDataStore()) }
    single { HabitRepository(get(), get()) }
    single { TodoRepository(get(), get()) }
    single { CalendarEventRepository(get(), get()) }
    single { JournalRepository(get()) }
    single { NotesRepository(get(), get()) }
    single { AlarmRepository(get()) }
    single { ReminderRepository(get()) }
    single { TimerRepository(get()) }
    single<VaultFileSync> { DesktopVaultFileSync() }
    single { SyncEngine(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { SyncClient(get(), desktopDeviceId) }
    single { SyncServer(get(), desktopDeviceId) }
    // Stage 13: resolved once at Koin start, same lifetime as the Tailscale
    // interface itself for a running process. Also read directly by main.kt
    // for SyncServer's bindHost — kept as two separate cheap lookups rather
    // than threading one value through, since main() runs before Koin's
    // module block would otherwise make this single available to it.
    single {
        val host = findTailscaleAddress()
        val port = (System.getProperty("bori.syncPort") ?: System.getProperty("rhythm.syncPort"))?.toIntOrNull() ?: DEFAULT_SYNC_PORT
        val addressForPairing = "${host ?: "127.0.0.1"}:$port"
        val display = if (host != null) addressForPairing else "Tailscale not detected (using $addressForPairing)"
        LocalSyncAddress(display = display, addressForPairing = addressForPairing)
    }
    single<SyncCoordinator> { JvmSyncCoordinator(get()) }
    single<PurchaseLauncher> { DesktopPurchaseLauncher() }
    single<QrCodeRenderer> { DesktopQrCodeRenderer() }
    single<ImageBitmapLoader> { DesktopImageBitmapLoader() }
    single<FilePicker> { DesktopFilePickerService() }
    single<com.apagon.rhythm.platform.GlassBlur> { com.apagon.rhythm.platform.DesktopGlassBlur() }
    viewModel { DesktopHabitViewModel(get(), get()) }
    // The Habits/Today screen's grouped/streak/%-done state — see
    // ref_notes for the layout-parity round this was activated in. Every
    // dependency below was already registered for other screens (Todo,
    // Calendar, Journal, Settings); HabitListViewModel itself just hadn't
    // been wired into Koin yet.
    viewModel { HabitListViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { TodoViewModel(get(), get(), get(), get()) }
    viewModel { RecentlyDeletedViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { DesktopCalendarViewModel(get(), get()) }
    viewModel { JournalViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { NotesViewModel(get(), get(), get()) }
    viewModel { NotebookDetailViewModel(get()) }
    viewModel { NotesSearchViewModel(get()) }
    viewModel { NoteEditorViewModel(get(), get(), get()) }

    // ── Stage 11: Settings ───────────────────────────────────────────────────
    single<BackupManaging> { DesktopBackupManager(get(), get()) }
    viewModel { DesktopSettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    // Custom accent/Crystal-room/Crystal-mesh colour pickers — ColorPickerViewModel itself was
    // already commonMain (ported alongside Stage 11) but never registered, since nothing called
    // it until the custom-colour picker UI landed.
    viewModel { com.apagon.rhythm.ui.util.ColorPickerViewModel(get()) }

    // ── Stage 6: cross-cutting platform shims ───────────────────────────────
    single<LocaleFormatting> { DesktopLocaleFormatting() }
    single<HapticAlerter> { DesktopHapticAlerter() }
    single<PhotoStorage> { DesktopPhotoStorage() }
    single { DesktopDeviceCalendarIntegration() } bind DeviceCalendarIntegration::class

    // ── Stage 12: Alarms/Timers/Reminders/Clock ─────────────────────────────
    single<ReminderScheduling> { DesktopReminderScheduling() }
    single { AlertCenter() }
    single { DesktopOsNotifier() }
    single { DesktopAlarmClockService(get(), get(), get(), get(), get()) }
    viewModel { AlarmViewModel(get(), get(), get(), get()) }
    viewModel { ReminderViewModel(get(), get()) }
    viewModel { TimerViewModel(get(), get(), get(), get()) }
}
