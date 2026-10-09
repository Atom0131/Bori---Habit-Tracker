package com.apagon.rhythm.data.db

import com.apagon.rhythm.platform.AppHome

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
import com.apagon.rhythm.data.model.EventReminder
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.NoteLink
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.TodoSubtask
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
    entities = [Habit::class, HabitCompletion::class, ChecklistItem::class, ChecklistItemCompletion::class, Todo::class, CalendarEvent::class, JournalEntry::class, Notebook::class, Note::class, Alarm::class, Reminder::class, Timer::class, EventReminder::class, NoteLink::class, TodoSubtask::class],
    version = 12,
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
    abstract fun eventReminderDao(): EventReminderDao
    abstract fun noteLinkDao(): NoteLinkDao
    abstract fun todoSubtaskDao(): TodoSubtaskDao
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

// Stage 1 of the backup/sync-prep plan: new tables for schema parity with Android's
// EventReminder/NoteLink/TodoSubtask — DAO + entity only, no repository/UI wiring this pass.
// None of the three entities declares @ColumnInfo(defaultValue=...), so (matching every prior
// brand-new-table migration in this file — DESKTOP_HABIT_MIGRATION_3_4/_4_5/_5_6/_6_7, all
// verified against their entities' actual annotations) this migration carries no SQL DEFAULT
// clauses. FKs/indices are copied directly from each entity's @Entity annotation:
// EventReminder (FK->calendar_events CASCADE, index on eventId), NoteLink (indices on both
// id columns, no FK), TodoSubtask (FK->todos CASCADE, indices on todoId and parentId, no FK
// on parentId — see TodoSubtask.kt's own KDoc for why).
val DESKTOP_HABIT_MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS event_reminders (
                id                 INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                eventId            INTEGER NOT NULL,
                minutesBefore      INTEGER,
                absoluteDateTime   TEXT,
                soundUri           TEXT NOT NULL,
                vibrationPatternId TEXT NOT NULL,
                createdAt          INTEGER NOT NULL,
                FOREIGN KEY(eventId) REFERENCES calendar_events(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_event_reminders_eventId ON event_reminders(eventId)")

        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS note_links (
                id           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sourceNoteId INTEGER NOT NULL,
                targetNoteId INTEGER NOT NULL
            )
        """.trimIndent())
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_note_links_sourceNoteId ON note_links(sourceNoteId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_note_links_targetNoteId ON note_links(targetNoteId)")

        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS todo_subtasks (
                id         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                todoId     INTEGER NOT NULL,
                label      TEXT NOT NULL,
                isDone     INTEGER NOT NULL,
                sortOrder  INTEGER NOT NULL,
                parentId   INTEGER,
                FOREIGN KEY(todoId) REFERENCES todos(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_todo_subtasks_todoId ON todo_subtasks(todoId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_todo_subtasks_parentId ON todo_subtasks(parentId)")
    }
}

// Stage 1: new columns on notebooks/notes/alarms/reminders for Android schema parity, all
// inert pass-through on desktop except Note.bodyPreview/uuid (see Note.kt). Column names are
// the Kotlin property names here — none of these four new fields uses @ColumnInfo(name=...)
// (unlike Alarm.repeatDaysMask/repeatDays, untouched by this migration). SQLite requires a
// literal DEFAULT on any NOT NULL column added to a non-empty table, so the four NOT-NULL
// additions (notebooks.isPrivate, notes.bodyPreview, alarms.dismissMission,
// alarms.missionDifficulty) carry one, matching each property's Kotlin default exactly; the
// nullable additions carry none, matching this file's established no-default convention.
val DESKTOP_HABIT_MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE notebooks ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE notebooks ADD COLUMN vaultFolderName TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE notebooks ADD COLUMN folderDocUriCache TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE notebooks ADD COLUMN parentId INTEGER DEFAULT NULL")

        connection.execSQL("ALTER TABLE notes ADD COLUMN bodyPreview TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE notes ADD COLUMN filePointer TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE notes ADD COLUMN fileDocUriCache TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE notes ADD COLUMN fileSyncedAt INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE notes ADD COLUMN uuid TEXT DEFAULT NULL")

        connection.execSQL("ALTER TABLE alarms ADD COLUMN dismissMission TEXT NOT NULL DEFAULT 'none'")
        connection.execSQL("ALTER TABLE alarms ADD COLUMN missionDifficulty INTEGER NOT NULL DEFAULT 1")

        connection.execSQL("ALTER TABLE reminders ADD COLUMN noteId INTEGER DEFAULT NULL")
    }
}

// Stage 1: one-time data migration remapping alarms.repeatDays from desktop's old bit order
// (bit0=Mon, bit1=Tue, ..., bit6=Sun) to Android's (bit0=Sun, bit1=Mon, ..., bit6=Sat) — see
// Alarm.kt's KDoc. Column name is `repeatDays` (the @ColumnInfo name), not `repeatDaysMask` —
// commonMain's (dead-code) MIGRATION_32_33 got this exact thing wrong.
//
// Every old bit N (weekday) must land at new bit (N+1) mod 7: old bit0=Mon -> new bit1=Mon,
// old bit1=Tue -> new bit2=Tue, ..., old bit5=Sat -> new bit6=Sat, old bit6=Sun -> new bit0=Sun.
// That is a left-rotate by one bit within the low 7 bits:
//   new = ((old << 1) | (old >> 6)) & 0x7F
// (mask to 7 bits first in case any stray high bits are present, which the app has never set).
//
// Hand-verified against two known values before use:
//   Mon+Wed, old = bit0|bit2 = 0b0000101 = 5.
//     (5*2)=10, 5/64=0 -> 10|0 = 10 = 0b0001010 = new bit1|bit3 = Mon|Wed. Correct (Mon,Wed
//     both still set, just moved to their new bit positions).
//   Sun+Sat, old = bit6|bit5 = 0b1100000 = 96.
//     (96*2)&127 = 192&127 = 64 = 0b1000000 = new bit6 (Sat). 96/64 = 1 = new bit0 (Sun).
//     64|1 = 65 = 0b1000001 = new bit0|bit6 = Sun|Sat. Correct.
val DESKTOP_HABIT_MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "UPDATE alarms SET repeatDays = " +
                "(((repeatDays & 127) * 2) & 127) | ((repeatDays & 127) / 64) " +
                "WHERE repeatDays != 0"
        )
    }
}

// Stage 1 of the full-entity-sync plan: adds syncId/updatedAt sync bookkeeping to every
// remaining entity (everything except Habit/HabitCompletion, which already got this in
// DESKTOP_HABIT_MIGRATION_1_2). Schema only — the sync engine itself isn't wired to these
// tables yet (that's Stage 2, a separate pass).
//
// Column-name check done against each entity's actual @ColumnInfo annotations before writing
// this (per the Alarm.repeatDaysMask/repeatDays lesson from DESKTOP_HABIT_MIGRATION_1_2/_9_10):
// none of the 13 entities touched here declare a @ColumnInfo(name=...) that differs from its
// Kotlin property name, so every ALTER/UPDATE below uses the property name directly.
//
// notebooks/notes/journal_entries already have `updatedAt` (used for sort order, not sync) —
// only `syncId` is added for those three, no second ALTER, to avoid a duplicate-column crash.
// The other ten entities get both columns. updatedAt is backfilled from `createdAt` where that
// column exists (todos, calendar_events, alarms, timers, reminders, event_reminders); the five
// full-replace child tables with neither syncId/updatedAt/createdAt before now (todo_subtasks,
// checklist_items, checklist_item_completions, note_links) backfill updatedAt from current time,
// same reasoning DESKTOP_HABIT_MIGRATION_1_2 used for habit_completions.
val DESKTOP_HABIT_MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        val now = com.apagon.rhythm.core.time.System.currentTimeMillis()

        // --- Entities with existing deletedAt, backfill updatedAt from createdAt ---
        connection.execSQL("ALTER TABLE todos ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE todos ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE todos SET updatedAt = createdAt WHERE updatedAt = 0")

        connection.execSQL("ALTER TABLE calendar_events ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE calendar_events ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE calendar_events SET updatedAt = createdAt WHERE updatedAt = 0")

        connection.execSQL("ALTER TABLE alarms ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE alarms ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE alarms SET updatedAt = createdAt WHERE updatedAt = 0")

        connection.execSQL("ALTER TABLE timers ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE timers ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE timers SET updatedAt = createdAt WHERE updatedAt = 0")

        connection.execSQL("ALTER TABLE reminders ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE reminders ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE reminders SET updatedAt = createdAt WHERE updatedAt = 0")

        // --- Entities that already have updatedAt: syncId only ---
        connection.execSQL("ALTER TABLE notebooks ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE notes ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE journal_entries ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")

        // --- Full-replace-per-parent child tables: both columns, no deletedAt, backfill updatedAt
        // from createdAt where that column exists, else from "now" ---
        connection.execSQL("ALTER TABLE event_reminders ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE event_reminders ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE event_reminders SET updatedAt = createdAt WHERE updatedAt = 0")

        connection.execSQL("ALTER TABLE todo_subtasks ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE todo_subtasks ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE todo_subtasks SET updatedAt = $now WHERE updatedAt = 0")

        connection.execSQL("ALTER TABLE checklist_items ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE checklist_items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE checklist_items SET updatedAt = $now WHERE updatedAt = 0")

        connection.execSQL("ALTER TABLE checklist_item_completions ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE checklist_item_completions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE checklist_item_completions SET updatedAt = $now WHERE updatedAt = 0")

        connection.execSQL("ALTER TABLE note_links ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE note_links ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE note_links SET updatedAt = $now WHERE updatedAt = 0")

        // --- Backfill real per-row UUIDs (SQL ADD COLUMN DEFAULT can't express "fresh UUID per row") ---
        backfillSyncIds(connection, "todos")
        backfillSyncIds(connection, "calendar_events")
        backfillSyncIds(connection, "alarms")
        backfillSyncIds(connection, "timers")
        backfillSyncIds(connection, "reminders")
        backfillSyncIds(connection, "notebooks")
        backfillSyncIds(connection, "notes")
        backfillSyncIds(connection, "journal_entries")
        backfillSyncIds(connection, "event_reminders")
        backfillSyncIds(connection, "todo_subtasks")
        backfillSyncIds(connection, "checklist_items")
        backfillSyncIds(connection, "checklist_item_completions")
        backfillSyncIds(connection, "note_links")

        // --- Unique indices on syncId ---
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_todos_syncId ON todos(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_calendar_events_syncId ON calendar_events(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_alarms_syncId ON alarms(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_timers_syncId ON timers(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reminders_syncId ON reminders(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notebooks_syncId ON notebooks(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notes_syncId ON notes(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_journal_entries_syncId ON journal_entries(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_event_reminders_syncId ON event_reminders(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_todo_subtasks_syncId ON todo_subtasks(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_checklist_items_syncId ON checklist_items(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_checklist_item_completions_syncId ON checklist_item_completions(syncId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_note_links_syncId ON note_links(syncId)")
    }
}

// Stage 2 of the full-entity-sync plan: the unsynced-notebook-by-name dedup fix (ported from
// Android's SyncEngine.kt, commit 6f58f5d) needs a way to tell "this notebook has never been
// exchanged with a peer" apart from "this notebook has a syncId" — unlike Android, desktop mints
// syncId eagerly at row creation (see DESKTOP_HABIT_MIGRATION_1_2/_10_11's backfillSyncIds), so
// syncId is never null here and can't serve as that signal the way it does on Android. See
// Notebook.firstSyncedAt's KDoc and SyncEngine.kt's notebook-matching block.
val DESKTOP_HABIT_MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE notebooks ADD COLUMN firstSyncedAt INTEGER DEFAULT NULL")
    }
}

fun buildDesktopHabitDatabase(): DesktopHabitDatabase {
    // -Dbori.home=<dir> overrides ~/.bori (see AppHome) — lets Stage 4b's local loopback
    // sync test run two independent "devices" as separate JVM processes on
    // this one machine, each pointed at its own database directory.
    val dbDir = AppHome.dir
    val dbFile = File(dbDir, "habit_database.db")
    return Room.databaseBuilder<DesktopHabitDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(DESKTOP_HABIT_MIGRATION_1_2, DESKTOP_HABIT_MIGRATION_2_3, DESKTOP_HABIT_MIGRATION_3_4, DESKTOP_HABIT_MIGRATION_4_5, DESKTOP_HABIT_MIGRATION_5_6, DESKTOP_HABIT_MIGRATION_6_7, DESKTOP_HABIT_MIGRATION_7_8, DESKTOP_HABIT_MIGRATION_8_9, DESKTOP_HABIT_MIGRATION_9_10, DESKTOP_HABIT_MIGRATION_10_11, DESKTOP_HABIT_MIGRATION_11_12)
        .build()
}
