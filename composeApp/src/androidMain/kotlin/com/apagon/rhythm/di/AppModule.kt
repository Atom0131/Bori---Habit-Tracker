package com.apagon.rhythm.di

import androidx.room.Room
import com.apagon.rhythm.data.CleanupManager
import com.apagon.rhythm.data.backup.BackupManager
import com.apagon.rhythm.data.billing.BillingRepository
import com.apagon.rhythm.data.db.HabitDatabase
import com.apagon.rhythm.data.db.MIGRATION_1_2
import com.apagon.rhythm.data.db.MIGRATION_2_3
import com.apagon.rhythm.data.db.MIGRATION_3_4
import com.apagon.rhythm.data.db.MIGRATION_4_5
import com.apagon.rhythm.data.db.MIGRATION_5_6
import com.apagon.rhythm.data.db.MIGRATION_6_7
import com.apagon.rhythm.data.db.MIGRATION_7_8
import com.apagon.rhythm.data.db.MIGRATION_8_9
import com.apagon.rhythm.data.db.MIGRATION_9_10
import com.apagon.rhythm.data.db.MIGRATION_10_11
import com.apagon.rhythm.data.db.MIGRATION_11_12
import com.apagon.rhythm.data.db.MIGRATION_12_13
import com.apagon.rhythm.data.db.MIGRATION_13_14
import com.apagon.rhythm.data.db.MIGRATION_14_15
import com.apagon.rhythm.data.db.MIGRATION_15_16
import com.apagon.rhythm.data.db.MIGRATION_16_17
import com.apagon.rhythm.data.db.MIGRATION_17_18
import com.apagon.rhythm.data.db.MIGRATION_18_19
import com.apagon.rhythm.data.db.MIGRATION_19_20
import com.apagon.rhythm.data.db.MIGRATION_20_21
import com.apagon.rhythm.data.db.MIGRATION_21_22
import com.apagon.rhythm.data.db.MIGRATION_22_23
import com.apagon.rhythm.data.db.MIGRATION_23_24
import com.apagon.rhythm.data.db.MIGRATION_24_25
import com.apagon.rhythm.data.db.MIGRATION_25_26
import com.apagon.rhythm.data.db.MIGRATION_26_27
import com.apagon.rhythm.data.db.MIGRATION_27_28
import com.apagon.rhythm.data.db.MIGRATION_28_29
import com.apagon.rhythm.data.db.MIGRATION_29_30
import com.apagon.rhythm.data.db.MIGRATION_30_31
import com.apagon.rhythm.data.db.MIGRATION_31_32
import com.apagon.rhythm.data.db.MIGRATION_32_33
import com.apagon.rhythm.data.db.MIGRATION_33_34
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.repository.AlarmRepository
import com.apagon.rhythm.data.repository.CalendarEventRepository
import com.apagon.rhythm.data.repository.CalendarIntegrationRepository
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.JournalRepository
import com.apagon.rhythm.data.repository.NotesRepository
import com.apagon.rhythm.data.repository.ReminderRepository
import com.apagon.rhythm.data.repository.SecurityRepository
import com.apagon.rhythm.data.repository.TimerRepository
import com.apagon.rhythm.data.repository.TodoRepository
import com.apagon.rhythm.data.sync.AndroidSyncPreferences
import com.apagon.rhythm.data.sync.SyncPreferences
import com.apagon.rhythm.ui.alarms.AlarmViewModel
import com.apagon.rhythm.ui.alarms.TimerViewModel
import com.apagon.rhythm.ui.calendar.CalendarViewModel
import com.apagon.rhythm.ui.deleted.RecentlyDeletedViewModel
import com.apagon.rhythm.ui.habit.HabitListViewModel
import com.apagon.rhythm.ui.journal.JournalViewModel
import com.apagon.rhythm.ui.notes.NoteEditorViewModel
import com.apagon.rhythm.ui.notes.NotebookDetailViewModel
import com.apagon.rhythm.ui.notes.NotesSearchViewModel
import com.apagon.rhythm.ui.notes.NotesViewModel
import com.apagon.rhythm.ui.reminders.ReminderViewModel
import com.apagon.rhythm.ui.settings.SettingsViewModel
import com.apagon.rhythm.ui.stats.StatsViewModel
import com.apagon.rhythm.ui.todos.TodoViewModel
import com.apagon.rhythm.platform.AndroidPurchaseLauncher
import com.apagon.rhythm.platform.AndroidReminderScheduling
import com.apagon.rhythm.platform.AndroidWidgetRefresher
import com.apagon.rhythm.platform.PurchaseLauncher
import com.apagon.rhythm.platform.ReminderScheduling
import com.apagon.rhythm.platform.WidgetRefresher
import com.apagon.rhythm.ui.util.ColorPickerViewModel
import com.apagon.rhythm.ui.util.PaywallViewModel
import com.apagon.rhythm.ui.util.TutorialViewModel
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

// Same names/paths as the old in-class delegates, so existing user data is preserved.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")
private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_prefs")

val appModule = module {

    // ── Database ──────────────────────────────────────────────────────────────
    single {
        Room.databaseBuilder(
            androidContext(),
            HabitDatabase::class.java,
            "habit_database"
        ).fallbackToDestructiveMigrationOnDowngrade()
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
                MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
                MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18,
                MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
                MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25,
                MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28,
                MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32,
                MIGRATION_32_33, MIGRATION_33_34
            ).build()
    }

    // ── DAOs ──────────────────────────────────────────────────────────────────
    single { get<HabitDatabase>().habitDao() }
    single { get<HabitDatabase>().reminderDao() }
    single { get<HabitDatabase>().calendarEventDao() }
    single { get<HabitDatabase>().alarmDao() }
    single { get<HabitDatabase>().timerDao() }
    single { get<HabitDatabase>().todoDao() }
    single { get<HabitDatabase>().journalDao() }
    single { get<HabitDatabase>().notesDao() }

    // ── Preferences / billing ─────────────────────────────────────────────────
    single { ThemePreferences(androidContext().settingsDataStore) }
    single { BillingRepository(androidContext(), get()) }
    single<SyncPreferences> { AndroidSyncPreferences(androidContext().syncDataStore) }

    // ── Repositories ──────────────────────────────────────────────────────────
    single { HabitRepository(get(), get()) }
    single { ReminderRepository(get()) }
    single { CalendarEventRepository(get()) }
    single { AlarmRepository(get()) }
    single { TimerRepository(get()) }
    single { TodoRepository(get()) }
    single { JournalRepository(get()) }
    single { NotesRepository(get()) }
    single { CalendarIntegrationRepository(androidContext()) } bind com.apagon.rhythm.data.repository.DeviceCalendarIntegration::class
    single { SecurityRepository(androidContext().securityDataStore) }

    // ── Platform services ─────────────────────────────────────────────────────
    single<ReminderScheduling> { AndroidReminderScheduling(androidContext()) }
    single<WidgetRefresher> { AndroidWidgetRefresher(androidContext()) }
    single { AndroidPurchaseLauncher(get()) } bind PurchaseLauncher::class
    single<com.apagon.rhythm.platform.PhotoStorage> { com.apagon.rhythm.platform.AndroidPhotoStorage(androidContext()) }
    single<com.apagon.rhythm.platform.LocaleFormatting> { com.apagon.rhythm.platform.AndroidLocaleFormatting(androidContext()) }
    single<com.apagon.rhythm.platform.HapticAlerter> { com.apagon.rhythm.platform.AndroidHapticAlerter(androidContext()) }

    // ── Managers ──────────────────────────────────────────────────────────────
    single { BackupManager(get(), get(), androidContext()) }
    single { CleanupManager(get(), get(), get(), get(), get(), get(), get(), get()) }

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel { HabitListViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CalendarViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { TodoViewModel(get(), get(), get(), get()) }
    viewModel { TimerViewModel(get(), get(), androidContext()) }
    viewModel { AlarmViewModel(get(), get(), get(), get()) }
    viewModel { ReminderViewModel(get(), get()) }
    viewModel { JournalViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(androidContext(), get(), get(), get(), get(), get(), get()) }
    viewModel { StatsViewModel(get()) }
    viewModel { NotesViewModel(get(), get(), get()) }
    viewModel { NotebookDetailViewModel(get()) }
    viewModel { NotesSearchViewModel(get()) }
    viewModel { NoteEditorViewModel(get(), get(), get()) }
    viewModel { RecentlyDeletedViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { PaywallViewModel(get()) }
    viewModel { ColorPickerViewModel(get()) }
    viewModel { TutorialViewModel(get()) }
}
