package com.apagon.rhythm.data.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Upgrades a real database file and checks nothing was lost. Runs only with
 * `-PmigrationDb=/path/to/a/COPY/of/habit_database.db` (the copy is copied again, never opened in place).
 */
class MigrationTest {

    @Test
    fun upgradesARealDatabase() {
        val src = System.getProperty("migrationDb")?.let(::File) ?: return
        val dir = Files.createTempDirectory("bori-mig").toFile()
        val file = File(dir, "habit_database.db")
        src.copyTo(file)
        fun count(table: String) = ProcessBuilder("sqlite3", file.path, "select count(*) from $table").start()
            .inputStream.bufferedReader().readText().trim().toInt()
        val before = listOf("calendar_events", "todos", "reminders", "habits", "notes").associateWith { count(it) }

        val db = Room.databaseBuilder<DesktopHabitDatabase>(name = file.absolutePath)
            .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(*DESKTOP_HABIT_MIGRATIONS).build()
        runBlocking {
            val events = db.calendarEventDao().getAllActiveEvents().first()
            assertTrue(events.all { it.recurrence == "" && it.recurrenceExceptions == "" })
            assertTrue(db.todoDao().getAllActiveTodos().first().all { it.recurrence == "" })
        }
        db.close()
        val after = before.keys.associateWith { count(it) }
        assertEquals(before, after)
        dir.deleteRecursively()
    }
}
