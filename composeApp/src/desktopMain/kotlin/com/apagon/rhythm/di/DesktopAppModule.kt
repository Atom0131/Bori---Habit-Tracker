package com.apagon.rhythm.di

import com.apagon.rhythm.data.db.DesktopHabitDatabase
import com.apagon.rhythm.data.db.buildDesktopHabitDatabase
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.preferences.buildDesktopSecurityDataStore
import com.apagon.rhythm.data.preferences.buildDesktopThemeDataStore
import com.apagon.rhythm.data.repository.CalendarEventRepository
import com.apagon.rhythm.data.repository.DesktopDeviceCalendarIntegration
import com.apagon.rhythm.data.repository.DeviceCalendarIntegration
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.JournalRepository
import com.apagon.rhythm.data.repository.SecurityRepository
import com.apagon.rhythm.data.repository.TodoRepository
import com.apagon.rhythm.data.sync.DesktopSyncPreferences
import com.apagon.rhythm.data.sync.JvmSyncCoordinator
import com.apagon.rhythm.data.sync.SyncClient
import com.apagon.rhythm.data.sync.SyncCoordinator
import com.apagon.rhythm.data.sync.SyncEngine
import com.apagon.rhythm.data.sync.SyncPreferences
import com.apagon.rhythm.data.sync.SyncServer
import com.apagon.rhythm.platform.DesktopHapticAlerter
import com.apagon.rhythm.platform.DesktopImageBitmapLoader
import com.apagon.rhythm.platform.DesktopLocaleFormatting
import com.apagon.rhythm.platform.DesktopNoOpReminderScheduling
import com.apagon.rhythm.platform.DesktopPhotoStorage
import com.apagon.rhythm.platform.DesktopPurchaseLauncher
import com.apagon.rhythm.platform.DesktopFilePickerService
import com.apagon.rhythm.platform.DesktopWidgetRefresher
import com.apagon.rhythm.platform.FilePicker
import com.apagon.rhythm.platform.HapticAlerter
import com.apagon.rhythm.platform.ImageBitmapLoader
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.platform.PhotoStorage
import com.apagon.rhythm.platform.PurchaseLauncher
import com.apagon.rhythm.platform.ReminderScheduling
import com.apagon.rhythm.platform.WidgetRefresher
import com.apagon.rhythm.ui.calendar.DesktopCalendarViewModel
import com.apagon.rhythm.ui.deleted.DesktopRecentlyDeletedViewModel
import com.apagon.rhythm.ui.habit.DesktopHabitViewModel
import com.apagon.rhythm.ui.journal.JournalViewModel
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
    single<WidgetRefresher> { DesktopWidgetRefresher() }
    single<SyncPreferences> { DesktopSyncPreferences() }
    single { ThemePreferences(buildDesktopThemeDataStore()) }
    single { SecurityRepository(buildDesktopSecurityDataStore()) }
    single { HabitRepository(get(), get()) }
    single { TodoRepository(get()) }
    single { CalendarEventRepository(get()) }
    single { JournalRepository(get()) }
    single { SyncEngine(get(), get()) }
    single { SyncClient(get(), desktopDeviceId) }
    single { SyncServer(get(), desktopDeviceId) }
    single<SyncCoordinator> { JvmSyncCoordinator(get()) }
    single<PurchaseLauncher> { DesktopPurchaseLauncher() }
    single<ImageBitmapLoader> { DesktopImageBitmapLoader() }
    single<FilePicker> { DesktopFilePickerService() }
    viewModel { DesktopHabitViewModel(get(), get(), get()) }
    viewModel { TodoViewModel(get(), get(), get(), get()) }
    viewModel { DesktopRecentlyDeletedViewModel(get(), get()) }
    viewModel { DesktopCalendarViewModel(get(), get()) }
    viewModel { JournalViewModel(get(), get(), get(), get(), get(), get()) }

    // ── Stage 6: cross-cutting platform shims ───────────────────────────────
    single<LocaleFormatting> { DesktopLocaleFormatting() }
    single<HapticAlerter> { DesktopHapticAlerter() }
    single<PhotoStorage> { DesktopPhotoStorage() }
    single { DesktopDeviceCalendarIntegration() } bind DeviceCalendarIntegration::class

    // ── Stage 7: temporary no-op scheduler (real one lands in Stage 12) ────
    single<ReminderScheduling> { DesktopNoOpReminderScheduling() }
}
