package com.apagon.rhythm.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistItemCompletion
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
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
    entities = [Habit::class, HabitCompletion::class, ChecklistItem::class, ChecklistItemCompletion::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(HabitFrequencyConverter::class)
abstract class DesktopHabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
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

fun buildDesktopHabitDatabase(): DesktopHabitDatabase {
    val dbDir = File(System.getProperty("user.home"), ".rhythm")
    dbDir.mkdirs()
    val dbFile = File(dbDir, "habit_database.db")
    return Room.databaseBuilder<DesktopHabitDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(DESKTOP_HABIT_MIGRATION_1_2)
        .build()
}
