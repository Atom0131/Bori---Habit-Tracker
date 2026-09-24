package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistItemCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // ── Habits ────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM habits WHERE isActive = 1 AND deletedAt IS NULL ORDER BY createdAt ASC")
    fun getAllActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE isActive = 0 AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllArchivedHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedHabits(): Flow<List<Habit>>

    @Query("DELETE FROM habits WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeDeletedHabits(olderThan: Long)

    // deletedAt IS NULL matters here: without it, a soft-deleted habit could
    // still be looked up by id and re-arm its own reminder. Ported forward
    // from the live Android app's fix for the same bug.
    @Query("SELECT * FROM habits WHERE id = :id AND deletedAt IS NULL")
    fun getHabitById(id: Long): Flow<Habit?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("UPDATE habits SET reminderTime = :time WHERE id = :habitId")
    suspend fun updateReminderTime(habitId: Long, time: String?)

    // ── Sync (Stage 4) ───────────────────────────────────────────────────────

    @Query("SELECT * FROM habits WHERE syncId = :syncId")
    suspend fun getHabitBySyncId(syncId: String): Habit?

    /** Unlike getHabitById, no deletedAt filter — a completion belonging to a soft-deleted habit still needs its habit's syncId resolved when building an outgoing sync batch. */
    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitByIdRaw(id: Long): Habit?

    @Query("SELECT * FROM habits WHERE updatedAt > :since")
    suspend fun getHabitsUpdatedSince(since: Long): List<Habit>

    @Query("SELECT * FROM habit_completions WHERE syncId = :syncId")
    suspend fun getCompletionBySyncId(syncId: String): HabitCompletion?

    @Query("SELECT * FROM habit_completions WHERE updatedAt > :since")
    suspend fun getCompletionsUpdatedSince(since: Long): List<HabitCompletion>

    @Query("DELETE FROM habit_completions WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeDeletedCompletions(olderThan: Long)

    // ── Completions ───────────────────────────────────────────────────────────

    // Deliberately NOT used for a plain "complete this habit" tap anymore —
    // OnConflictStrategy.IGNORE silently no-ops if a tombstoned row (see
    // HabitCompletion.deletedAt) still occupies the unique (habitId,
    // dateCompleted) slot, which re-completing a previously-unchecked day
    // does. HabitRepository.markComplete does a revive-or-insert using the
    // two methods below instead; this stays for sync's fresh-row inserts and
    // backup/restore, where no prior tombstone can exist.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: HabitCompletion): Long

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateCompleted = :date")
    suspend fun getCompletionRaw(habitId: Long, date: String): HabitCompletion?

    /** Overwrites deletedAt/updatedAt exactly as given. A local soft-delete passes a non-null deletedAt; a local revive (re-completing a day) or applying a remote sync row passes whatever it says (null = active). */
    @Query("UPDATE habit_completions SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE habitId = :habitId AND dateCompleted = :date")
    suspend fun setCompletionSyncState(habitId: Long, date: String, deletedAt: Long?, updatedAt: Long)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateCompleted = :date")
    suspend fun deleteCompletion(habitId: Long, date: String)

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND deletedAt IS NULL ORDER BY dateCompleted DESC")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE dateCompleted = :date AND deletedAt IS NULL")
    fun getCompletionsByDate(date: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE dateCompleted >= :startDate AND dateCompleted <= :endDate AND deletedAt IS NULL")
    fun getCompletionsBetweenDates(startDate: String, endDate: String): Flow<List<HabitCompletion>>

    // ── Checklist Items ───────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItem>)

    @Delete
    suspend fun deleteChecklistItems(items: List<ChecklistItem>)

    @Update
    suspend fun updateChecklistItems(items: List<ChecklistItem>)

    @Query("SELECT * FROM checklist_items WHERE habitId = :habitId ORDER BY sortOrder ASC")
    fun getItemsForHabit(habitId: Long): Flow<List<ChecklistItem>>

    @Query("DELETE FROM checklist_items WHERE habitId = :habitId")
    suspend fun deleteAllItemsForHabit(habitId: Long)

    // ── Checklist Item Completions ────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemCompletion(completion: ChecklistItemCompletion): Long

    @Query("DELETE FROM checklist_item_completions WHERE itemId = :itemId AND dateCompleted = :date")
    suspend fun deleteItemCompletion(itemId: Long, date: String)

    @Query("""
        SELECT cic.* FROM checklist_item_completions cic
        JOIN checklist_items ci ON ci.id = cic.itemId
        WHERE ci.habitId IN (:habitIds)
        AND cic.dateCompleted = :date
    """)
    fun getItemCompletionsByHabitsOnDate(habitIds: List<Long>, date: String): Flow<List<ChecklistItemCompletion>>

    @Query("""
        SELECT cic.* FROM checklist_item_completions cic
        JOIN checklist_items ci ON ci.id = cic.itemId
        WHERE ci.habitId IN (:habitIds)
        AND cic.dateCompleted = :date
    """)
    suspend fun getItemCompletionsByHabitsOnDateSync(habitIds: List<Long>, date: String): List<ChecklistItemCompletion>

    // ── Backup / Restore ──────────────────────────────────────────────────────

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsForBackup(): List<Habit>

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllCompletionsForBackup(): List<HabitCompletion>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllChecklistItemsForBackup(): List<ChecklistItem>

    @Query("SELECT * FROM checklist_item_completions")
    suspend fun getAllChecklistItemCompletionsForBackup(): List<ChecklistItemCompletion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHabits(habits: List<Habit>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCompletions(completions: List<HabitCompletion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChecklistItemCompletions(completions: List<ChecklistItemCompletion>)

    @Query("DELETE FROM checklist_item_completions")
    suspend fun deleteAllChecklistItemCompletions()

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAllChecklistItems()

    @Query("DELETE FROM habit_completions")
    suspend fun deleteAllCompletions()

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()
}
