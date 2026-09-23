package com.apagon.rhythm.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.room.ConstructedBy
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistItemCompletion
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.model.Note

class HabitFrequencyConverter {
    @TypeConverter
    fun fromFrequency(frequency: HabitFrequency): String = frequency.name

    @TypeConverter
    fun toFrequency(value: String): HabitFrequency = HabitFrequency.valueOf(value)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE habits ADD COLUMN weekDaysMask INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE habits ADD COLUMN monthDaysMask INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE habits ADD COLUMN isChecklist INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS checklist_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                habitId INTEGER NOT NULL,
                label TEXT NOT NULL,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(habitId) REFERENCES habits(id) ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_checklist_items_habitId ON checklist_items(habitId)"
        )
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS checklist_item_completions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                itemId INTEGER NOT NULL,
                dateCompleted TEXT NOT NULL,
                FOREIGN KEY(itemId) REFERENCES checklist_items(id) ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_cic_itemId ON checklist_item_completions(itemId)"
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_cic_itemId_date ON checklist_item_completions(itemId, dateCompleted)"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                dateTime TEXT NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE reminders ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE habits ADD COLUMN colorIndex INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS calendar_events (
                id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title       TEXT NOT NULL,
                note        TEXT NOT NULL,
                startDate   TEXT NOT NULL,
                endDate     TEXT NOT NULL,
                startTime   TEXT,
                endTime     TEXT,
                colorIndex  INTEGER NOT NULL DEFAULT 0,
                isActive    INTEGER NOT NULL DEFAULT 1,
                createdAt   INTEGER NOT NULL
            )
        """.trimIndent())
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_calendar_events_startDate ON calendar_events(startDate)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_calendar_events_endDate ON calendar_events(endDate)"
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE reminders ADD COLUMN soundUri TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        // One-time data migration: convert pre-existing MONTHLY records to DAILY.
        connection.execSQL("UPDATE habits SET frequency = 'DAILY' WHERE frequency = 'MONTHLY'")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE habits ADD COLUMN durationDays INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS alarms (
                id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                label       TEXT NOT NULL DEFAULT '',
                hour        INTEGER NOT NULL,
                minute      INTEGER NOT NULL,
                repeatDays  INTEGER NOT NULL DEFAULT 0,
                isEnabled   INTEGER NOT NULL DEFAULT 1,
                soundUri    TEXT NOT NULL DEFAULT '',
                createdAt   INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS timers (
                id                INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                label             TEXT NOT NULL DEFAULT '',
                durationSeconds   INTEGER NOT NULL,
                remainingSeconds  INTEGER NOT NULL,
                createdAt         INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE timers ADD COLUMN endTimeMillis INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE timers ADD COLUMN soundUri TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS todos (
                id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title       TEXT NOT NULL,
                note        TEXT NOT NULL DEFAULT '',
                dueDate     TEXT NOT NULL DEFAULT '',
                priority    TEXT NOT NULL DEFAULT 'NONE',
                isCompleted INTEGER NOT NULL DEFAULT 0,
                createdAt   INTEGER NOT NULL
            )
        """.trimIndent())
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_todos_isCompleted ON todos(isCompleted)"
        )
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE todos ADD COLUMN iconIndex INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS journal_entries (
                id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date        TEXT NOT NULL,
                title       TEXT NOT NULL DEFAULT '',
                content     TEXT NOT NULL DEFAULT '',
                mood        INTEGER NOT NULL DEFAULT 0,
                habitId     INTEGER,
                createdAt   INTEGER NOT NULL,
                updatedAt   INTEGER NOT NULL,
                FOREIGN KEY(habitId) REFERENCES habits(id) ON DELETE SET NULL
            )
        """.trimIndent())
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_journal_entries_habitId ON journal_entries(habitId)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_journal_entries_date ON journal_entries(date)"
        )
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE journal_entries ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE journal_entries ADD COLUMN photoUris TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE journal_entries ADD COLUMN feelings TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE habits ADD COLUMN colorArgb INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE calendar_events ADD COLUMN colorArgb INTEGER DEFAULT NULL")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE habits ADD COLUMN iconIndex INTEGER NOT NULL DEFAULT -1")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE timers ADD COLUMN isPomo INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE timers ADD COLUMN pomoWorkSecs INTEGER NOT NULL DEFAULT 1500")
        connection.execSQL("ALTER TABLE timers ADD COLUMN pomoShortBreakSecs INTEGER NOT NULL DEFAULT 300")
        connection.execSQL("ALTER TABLE timers ADD COLUMN pomoLongBreakSecs INTEGER NOT NULL DEFAULT 900")
        connection.execSQL("ALTER TABLE timers ADD COLUMN pomoSessionsPerRound INTEGER NOT NULL DEFAULT 4")
        connection.execSQL("ALTER TABLE timers ADD COLUMN pomoCurrentSession INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE timers ADD COLUMN pomoPhase TEXT NOT NULL DEFAULT 'WORK'")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS notebooks (
                id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name        TEXT NOT NULL,
                colorIndex  INTEGER NOT NULL DEFAULT 0,
                colorArgb   INTEGER,
                createdAt   INTEGER NOT NULL,
                updatedAt   INTEGER NOT NULL
            )
        """.trimIndent())
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS notes (
                id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                notebookId  INTEGER NOT NULL,
                title       TEXT NOT NULL,
                content     TEXT NOT NULL,
                createdAt   INTEGER NOT NULL,
                updatedAt   INTEGER NOT NULL,
                isPinned    INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(notebookId) REFERENCES notebooks(id) ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_notes_notebookId ON notes(notebookId)"
        )
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE reminders ADD COLUMN completedAt INTEGER DEFAULT NULL")
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE todos ADD COLUMN completedAt INTEGER DEFAULT NULL")
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE habits ADD COLUMN deletedAt INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE calendar_events ADD COLUMN deletedAt INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE alarms ADD COLUMN deletedAt INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE timers ADD COLUMN deletedAt INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE todos ADD COLUMN deletedAt INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE reminders ADD COLUMN deletedAt INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE journal_entries ADD COLUMN deletedAt INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE notebooks ADD COLUMN deletedAt INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER DEFAULT NULL")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE todos ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_habit_completions_dateCompleted ON habit_completions(dateCompleted)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_todos_completedAt ON todos(completedAt)")
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE notes ADD COLUMN fontFamily TEXT NOT NULL DEFAULT 'default'")
        connection.execSQL("ALTER TABLE notes ADD COLUMN fontSize TEXT NOT NULL DEFAULT 'normal'")
    }
}

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE todos ADD COLUMN soundUri TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE habits ADD COLUMN soundUri TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE alarms ADD COLUMN vibrationPatternId TEXT NOT NULL DEFAULT 'default'")
        connection.execSQL("ALTER TABLE timers ADD COLUMN vibrationPatternId TEXT NOT NULL DEFAULT 'default'")
        connection.execSQL("ALTER TABLE habits ADD COLUMN vibrationPatternId TEXT NOT NULL DEFAULT 'default'")
        connection.execSQL("ALTER TABLE todos  ADD COLUMN vibrationPatternId TEXT NOT NULL DEFAULT 'default'")
    }
}

val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(connection: SQLiteConnection) {
        // Remap alarm repeat-day bits from Mon-based (bit0=Mon…bit6=Sun)
        // to Sun-based (bit0=Sun, bit1=Mon…bit6=Sat) to match ISO display order.
        // new = ((old & 63) << 1) | ((old >> 6) & 1)
        connection.execSQL(
            "UPDATE alarms SET repeatDaysMask = ((repeatDaysMask & 63) << 1) | ((repeatDaysMask >> 6) & 1) WHERE repeatDaysMask != 0"
        )
    }
}

@Database(
    entities = [Habit::class, HabitCompletion::class, ChecklistItem::class, ChecklistItemCompletion::class, Reminder::class, CalendarEvent::class, Alarm::class, Timer::class, Todo::class, JournalEntry::class, Notebook::class, Note::class],
    version = 33,
    exportSchema = false
)
@TypeConverters(HabitFrequencyConverter::class)
@ConstructedBy(HabitDatabaseConstructor::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun reminderDao(): ReminderDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun alarmDao(): AlarmDao
    abstract fun timerDao(): TimerDao
    abstract fun todoDao(): TodoDao
    abstract fun journalDao(): JournalDao
    abstract fun notesDao(): NotesDao
}


// KSP generates the platform actuals for this (Android/iOS database constructors).
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object HabitDatabaseConstructor : RoomDatabaseConstructor<HabitDatabase> {
    override fun initialize(): HabitDatabase
}
