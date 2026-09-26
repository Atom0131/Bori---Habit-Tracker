package com.apagon.rhythm.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistItemCompletion
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.data.model.Todo
import kotlinx.coroutines.Dispatchers
import java.io.File

// A brand-new, desktop-only database — this is a fresh install with nothing
// to migrate from, so it started at version 1 scoped to just the Habit
// vertical slice (Stage 2 of the Linux desktop plan). Deliberately NOT the
// same class as commonMain's HabitDatabase (12 entities, 32 migrations,
// shared with the still-configured androidTarget) — trimming that shared
// class would have broken Android's compile (its DI module and other
// repositories reference the other DAOs). Later phases that port more
// verticals to desktop add entities and desktop-native migrations here,
// starting from whatever version this database is actually at by then —
// they don't need to match Android's schema version.
@Database(
    entities = [Habit::class, HabitCompletion::class, ChecklistItem::class, ChecklistItemCompletion::class, Todo::class, CalendarEvent::class, JournalEntry::class, Notebook::class, Note::class, Alarm::class, Reminder::class, Timer::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(HabitFrequencyConverter::class)
abstract class DesktopHabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun todoDao(): TodoDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun journalDao(): JournalDao
    abstract fun notesDao(): NotesDao
    abstract fun alarmDao(): AlarmDao
    abstract fun reminderDao(): ReminderDao
    abstract fun timerDao(): TimerDao
}

// Same sync-bookkeeping columns as commonMain's MIGRATION_33_34, applied to
// desktop's own version timeline (this database's real Stage-3 data already
// sits on disk at ~/.rhythm/habit_database.db, so this must be a real
// migration, not a destructive schema rebuild — that would erase it). Named
// distinctly from commonMain's MIGRATION_1_2 (same package, but a different
// `@Database` class/timeline) to avoid a top-level declaration clash in the
// desktop compilation, which includes both files.
val DESKTOP_HABIT_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE habits ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE habits ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE habit_completions ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE habit_completions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE habit_completions ADD COLUMN deletedAt INTEGER DEFAULT NULL")

        connection.execSQL("UPDATE habits SET updatedAt = createdAt WHERE updatedAt = 0")
        connection.execSQL("UPDATE habit_completions SET updatedAt = ${com.apagon.rhythm.core.time.System.currentTimeMillis()} WHERE updatedAt = 0")

        backfillSyncIds(connection, "habits")
        backfillSyncIds(connection, "habit_completions")

        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habits_syncId ON habits(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_completions_syncId ON habit_completions(syncId)")
    }
}

// Stage 7: adds the `todos` table, brand-new on this database (no existing
// desktop rows to preserve for it), so this creates the entity's final
// current shape directly rather than replaying commonMain's incremental
// MIGRATION_14_15..MIGRATION_33_34 ALTERs one at a time. Column
// definitions/index names must match Room's generated schema for `Todo`
// exactly (verified by Room's runtime identity-hash check), same
// requirement DESKTOP_HABIT_MIGRATION_1_2 above already meets.
val DESKTOP_HABIT_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS todos (
                id                 INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title              TEXT NOT NULL,
                note               TEXT NOT NULL DEFAULT '',
                dueDate            TEXT NOT NULL DEFAULT '',
                priority           TEXT NOT NULL DEFAULT 'NONE',
                isCompleted        INTEGER NOT NULL DEFAULT 0,
                completedAt        INTEGER DEFAULT NULL,
                iconIndex          INTEGER NOT NULL DEFAULT 0,
                createdAt          INTEGER NOT NULL,
                isArchived         INTEGER NOT NULL DEFAULT 0,
                deletedAt          INTEGER DEFAULT NULL,
                soundUri           TEXT NOT NULL DEFAULT '',
                vibrationPatternId TEXT NOT NULL DEFAULT 'default'
            )
        """.trimIndent())
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_todos_isCompleted ON todos(isCompleted)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_todos_completedAt ON todos(completedAt)")
    }
}

// Stage 8: adds the `calendar_events` table, brand-new on this database.
// Unlike DESKTOP_HABIT_MIGRATION_2_3's `todos` table, CalendarEvent has no
// @ColumnInfo(defaultValue=...)/@Index annotations, so Room's expected
// TableInfo has no SQL DEFAULT clauses and no indices at all — verified by
// the exact "Migration didn't properly handle" diff Room prints on a
// mismatch. Match that shape exactly rather than decorating columns with
// defaults derived from the Kotlin data class's constructor defaults (those
// aren't the same thing to Room). No syncId/updatedAt columns either —
// calendar_events isn't part of the sync engine yet.
val DESKTOP_HABIT_MIGRATION_3_4 = object : Migration(3, 4) {
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
                colorIndex  INTEGER NOT NULL,
                colorArgb   INTEGER,
                isActive    INTEGER NOT NULL,
                createdAt   INTEGER NOT NULL,
                deletedAt   INTEGER
            )
        """.trimIndent())
    }
}

// Stage 9: adds the `journal_entries` table, brand-new on this database.
// Unlike CalendarEvent, JournalEntry DOES declare `indices = [Index("habitId"),
// Index("date")]` AND a @ForeignKey(habits, onDelete=SET_NULL) in its @Entity
// annotation — applying the Stage 8 lesson literally (match the specific
// entity's annotations, don't infer by analogy from a previous migration),
// this migration includes both indices, no SQL DEFAULT clauses (JournalEntry
// has no @ColumnInfo(defaultValue=...)), AND a FOREIGN KEY clause. The first
// attempt at this migration omitted the FK, guessing (wrongly) that no prior
// desktop migration needing one meant none was needed here either — Room's
// own "Migration didn't properly handle" Expected/Found diff on first launch
// caught it directly: Expected carried a ForeignKey{referenceTable='habits',
// onDelete='SET NULL'}, Found had none. Every other column/index matched.
val DESKTOP_HABIT_MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS journal_entries (
                id         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date       TEXT NOT NULL,
                title      TEXT NOT NULL,
                content    TEXT NOT NULL,
                mood       INTEGER NOT NULL,
                habitId    INTEGER,
                createdAt  INTEGER NOT NULL,
                updatedAt  INTEGER NOT NULL,
                tags       TEXT NOT NULL,
                photoUris  TEXT NOT NULL,
                feelings   TEXT NOT NULL,
                deletedAt  INTEGER,
                FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_journal_entries_habitId ON journal_entries(habitId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_journal_entries_date ON journal_entries(date)")
    }
}

// Stage 10: adds `notebooks` and `notes`, brand-new on this database.
// `Notebook` has no @ForeignKey/@Index/@ColumnInfo(defaultValue=...) — same
// shape as CalendarEvent, so no FK/index/DEFAULT clauses. `Note` DOES declare
// `@ForeignKey(Notebook::class, onDelete = CASCADE)` and `indices =
// [Index("notebookId")]` — same shape as JournalEntry (Stage 9), so this
// migration includes both, applying the same lesson: read the entity's own
// annotations, never infer from a sibling migration.
val DESKTOP_HABIT_MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS notebooks (
                id         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name       TEXT NOT NULL,
                colorIndex INTEGER NOT NULL,
                colorArgb  INTEGER,
                createdAt  INTEGER NOT NULL,
                updatedAt  INTEGER NOT NULL,
                deletedAt  INTEGER
            )
        """.trimIndent())
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS notes (
                id         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                notebookId INTEGER NOT NULL,
                title      TEXT NOT NULL,
                content    TEXT NOT NULL,
                createdAt  INTEGER NOT NULL,
                updatedAt  INTEGER NOT NULL,
                isPinned   INTEGER NOT NULL,
                deletedAt  INTEGER,
                tags       TEXT NOT NULL,
                fontFamily TEXT NOT NULL,
                fontSize   TEXT NOT NULL,
                FOREIGN KEY(notebookId) REFERENCES notebooks(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_notes_notebookId ON notes(notebookId)")
    }
}

// Stage 12: adds `alarms`, `reminders`, `timers` — brand-new on this database.
// None of the three declares `indices`/`@ForeignKey`/`@ColumnInfo(defaultValue=...)`
// in its @Entity annotation (confirmed by reading Alarm.kt/Reminder.kt/Timer.kt
// directly, not inferred from a sibling migration — Stage 9's own lesson), so
// this migration has no SQL DEFAULT clauses and no indices, matching
// DESKTOP_HABIT_MIGRATION_3_4's shape (CalendarEvent) rather than
// DESKTOP_HABIT_MIGRATION_2_3's (Todo, which does have defaultValue annotations).
val DESKTOP_HABIT_MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS alarms (
                id                 INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                label              TEXT NOT NULL,
                hour               INTEGER NOT NULL,
                minute             INTEGER NOT NULL,
                repeatDays         INTEGER NOT NULL,
                isEnabled          INTEGER NOT NULL,
                soundUri           TEXT NOT NULL,
                vibrationPatternId TEXT NOT NULL,
                createdAt          INTEGER NOT NULL,
                deletedAt          INTEGER
            )
        """.trimIndent())
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS reminders (
                id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title       TEXT NOT NULL,
                note        TEXT NOT NULL,
                dateTime    TEXT NOT NULL,
                isCompleted INTEGER NOT NULL,
                completedAt INTEGER,
                isActive    INTEGER NOT NULL,
                createdAt   INTEGER NOT NULL,
                soundUri    TEXT NOT NULL,
                deletedAt   INTEGER
            )
        """.trimIndent())
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS timers (
                id                   INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                label                TEXT NOT NULL,
                durationSeconds      INTEGER NOT NULL,
                remainingSeconds     INTEGER NOT NULL,
                endTimeMillis        INTEGER NOT NULL,
                soundUri             TEXT NOT NULL,
                vibrationPatternId   TEXT NOT NULL,
                createdAt            INTEGER NOT NULL,
                isPomo               INTEGER NOT NULL,
                pomoWorkSecs         INTEGER NOT NULL,
                pomoShortBreakSecs   INTEGER NOT NULL,
                pomoLongBreakSecs    INTEGER NOT NULL,
                pomoSessionsPerRound INTEGER NOT NULL,
                pomoCurrentSession   INTEGER NOT NULL,
                pomoPhase            TEXT NOT NULL,
                deletedAt            INTEGER
            )
        """.trimIndent())
    }
}

fun buildDesktopHabitDatabase(): DesktopHabitDatabase {
    // -Drhythm.home=<dir> overrides ~/.rhythm — lets Stage 4b's local loopback
    // sync test run two independent "devices" as separate JVM processes on
    // this one machine, each pointed at its own database directory.
    val dbDir = System.getProperty("rhythm.home")?.let { File(it) }
        ?: File(System.getProperty("user.home"), ".rhythm")
    dbDir.mkdirs()
    val dbFile = File(dbDir, "habit_database.db")
    return Room.databaseBuilder<DesktopHabitDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(DESKTOP_HABIT_MIGRATION_1_2, DESKTOP_HABIT_MIGRATION_2_3, DESKTOP_HABIT_MIGRATION_3_4, DESKTOP_HABIT_MIGRATION_4_5, DESKTOP_HABIT_MIGRATION_5_6, DESKTOP_HABIT_MIGRATION_6_7)
        .build()
}
