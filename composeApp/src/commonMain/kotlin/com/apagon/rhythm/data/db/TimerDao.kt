package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.apagon.rhythm.data.model.Timer
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    fun getAllTimers(): Flow<List<Timer>>

    @Query("SELECT * FROM timers WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedTimers(): Flow<List<Timer>>

    @Query("DELETE FROM timers WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeDeletedTimers(olderThan: Long)

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun getById(id: Long): Timer?

    @Insert
    suspend fun insert(timer: Timer): Long

    @Update
    suspend fun update(timer: Timer)

    @Delete
    suspend fun delete(timer: Timer)

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    @Query("SELECT * FROM timers WHERE syncId = :syncId")
    suspend fun getTimerBySyncId(syncId: String): Timer?

    @Query("SELECT * FROM timers WHERE updatedAt > :since")
    suspend fun getTimersUpdatedSince(since: Long): List<Timer>

    @Query("SELECT * FROM timers")
    suspend fun getAllTimersForBackup(): List<Timer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(timers: List<Timer>)

    @Query("DELETE FROM timers")
    suspend fun deleteAll()
}
