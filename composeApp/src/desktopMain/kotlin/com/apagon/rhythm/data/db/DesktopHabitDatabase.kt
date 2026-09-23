package com.apagon.rhythm.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistItemCompletion
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import kotlinx.coroutines.Dispatchers
import java.io.File

// A brand-new, desktop-only database — this is a fresh install with nothing
// to migrate from, so it starts at version 1 scoped to just the Habit
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
    version = 1,
    exportSchema = false
)
@TypeConverters(HabitFrequencyConverter::class)
abstract class DesktopHabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}

fun buildDesktopHabitDatabase(): DesktopHabitDatabase {
    val dbDir = File(System.getProperty("user.home"), ".rhythm")
    dbDir.mkdirs()
    val dbFile = File(dbDir, "habit_database.db")
    return Room.databaseBuilder<DesktopHabitDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
