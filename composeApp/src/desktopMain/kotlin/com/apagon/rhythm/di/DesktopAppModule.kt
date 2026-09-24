package com.apagon.rhythm.di

import com.apagon.rhythm.data.db.DesktopHabitDatabase
import com.apagon.rhythm.data.db.buildDesktopHabitDatabase
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.preferences.buildDesktopThemeDataStore
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.sync.DesktopSyncPreferences
import com.apagon.rhythm.data.sync.JvmSyncCoordinator
import com.apagon.rhythm.data.sync.SyncClient
import com.apagon.rhythm.data.sync.SyncCoordinator
import com.apagon.rhythm.data.sync.SyncEngine
import com.apagon.rhythm.data.sync.SyncPreferences
import com.apagon.rhythm.data.sync.SyncServer
import com.apagon.rhythm.platform.DesktopWidgetRefresher
import com.apagon.rhythm.platform.WidgetRefresher
import com.apagon.rhythm.ui.habit.DesktopHabitViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// A fresh id per process launch — good enough for Stage 4b/4c's metadata
// (SyncBatch.deviceId isn't used for identity/dedup logic, only diagnostics),
// so it's deliberately not persisted.
@OptIn(ExperimentalUuidApi::class)
private val desktopDeviceId = "desktop-" + Uuid.random().toString().take(8)

// Mirrors androidMain/di/AppModule.kt's shape but scoped to just what's been
// ported to desktop so far: the Habit database/DAO/repository, a no-op
// WidgetRefresher, and (Stage 5) real ThemePreferences persistence.
// Notifications, billing, calendar integration, the Notes vault, Journal,
// Todos, Alarms, and Timers are still deliberately not wired here — see
// ref_notes/ for the staged full-port roadmap.
val desktopAppModule = module {
    single<DesktopHabitDatabase> { buildDesktopHabitDatabase() }
    single { get<DesktopHabitDatabase>().habitDao() }
    single<WidgetRefresher> { DesktopWidgetRefresher() }
    single<SyncPreferences> { DesktopSyncPreferences() }
    single { ThemePreferences(buildDesktopThemeDataStore()) }
    single { HabitRepository(get(), get()) }
    single { SyncEngine(get(), get()) }
    single { SyncClient(get(), desktopDeviceId) }
    single { SyncServer(get(), desktopDeviceId) }
    single<SyncCoordinator> { JvmSyncCoordinator(get()) }
    viewModel { DesktopHabitViewModel(get(), get(), get()) }
}
