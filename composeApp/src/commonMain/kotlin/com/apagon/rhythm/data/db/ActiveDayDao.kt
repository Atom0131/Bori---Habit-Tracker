package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.apagon.rhythm.data.model.ActiveDay
import kotlinx.coroutines.flow.Flow

/**
 * The never-resetting day counter's record ([ActiveDay]). A DAO of its own rather than more queries
 * on [HabitDao], because `HabitDao` is also compiled into commonMain's `HabitDatabase`, which has no
 * `active_days` table — Room would reject the queries there. Only `DesktopHabitDatabase` exposes it.
 */
@Dao
interface ActiveDayDao {
    /** The Today card's day counter: every day with a habit done, up to [today]. Never resets. */
    @Query("SELECT COUNT(*) FROM active_days WHERE date <= :today")
    fun countThrough(today: String): Flow<Int>

    @Query("SELECT date FROM active_days ORDER BY date")
    suspend fun getAll(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(days: List<ActiveDay>)

    @Query("DELETE FROM active_days")
    suspend fun deleteAll()
}

/**
 * Keeps `active_days` in step with `habit_completions` — the same three triggers as the Android
 * app. Installed with `IF NOT EXISTS` on every open, so a fresh database and a migrated one both
 * get them; Room's schema check never sees triggers.
 *
 * There is deliberately **no DELETE trigger**: a completion is only hard-deleted by the purge of a
 * deleted habit (FK cascade) or by a restore, and neither may lower the count. An uncheck is a soft
 * delete (`HabitRepository.markIncomplete`), the one way a day can leave — and only when no other
 * habit is still done that day.
 */
object ActiveDayTriggers {
    private val ALL = listOf(
        "CREATE TRIGGER IF NOT EXISTS `active_day_on_insert` AFTER INSERT ON `habit_completions` " +
            "WHEN NEW.`deletedAt` IS NULL BEGIN " +
            "INSERT OR IGNORE INTO `active_days` (`date`) VALUES (NEW.`dateCompleted`); END",
        "CREATE TRIGGER IF NOT EXISTS `active_day_on_redo` AFTER UPDATE OF `deletedAt` ON `habit_completions` " +
            "WHEN NEW.`deletedAt` IS NULL BEGIN " +
            "INSERT OR IGNORE INTO `active_days` (`date`) VALUES (NEW.`dateCompleted`); END",
        "CREATE TRIGGER IF NOT EXISTS `active_day_on_undo` AFTER UPDATE OF `deletedAt` ON `habit_completions` " +
            "WHEN NEW.`deletedAt` IS NOT NULL AND OLD.`deletedAt` IS NULL BEGIN " +
            "DELETE FROM `active_days` WHERE `date` = NEW.`dateCompleted` AND NOT EXISTS (" +
            "SELECT 1 FROM `habit_completions` WHERE `dateCompleted` = NEW.`dateCompleted` " +
            "AND `deletedAt` IS NULL); END"
    )

    fun install(connection: SQLiteConnection) = ALL.forEach { connection.execSQL(it) }
}
