package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.apagon.rhythm.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND deletedAt IS NULL ORDER BY dateTime ASC")
    fun getAllActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND deletedAt IS NULL AND completedAt >= :dayStart AND completedAt < :dayEnd ORDER BY completedAt DESC")
    fun getCompletedRemindersForDay(dayStart: Long, dayEnd: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedReminders(): Flow<List<Reminder>>

    @Query("DELETE FROM reminders WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeDeletedReminders(olderThan: Long)

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    @Query("SELECT * FROM reminders WHERE syncId = :syncId")
    suspend fun getReminderBySyncId(syncId: String): Reminder?

    @Query("SELECT * FROM reminders WHERE updatedAt > :since")
    suspend fun getRemindersUpdatedSince(since: Long): List<Reminder>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersForBackup(): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<Reminder>)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
