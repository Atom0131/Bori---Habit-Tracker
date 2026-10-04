package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.apagon.rhythm.data.model.Alarm
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms WHERE deletedAt IS NULL ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND deletedAt IS NULL")
    suspend fun getEnabledAlarms(): List<Alarm>

    @Query("SELECT * FROM alarms WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedAlarms(): Flow<List<Alarm>>

    @Query("DELETE FROM alarms WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeDeletedAlarms(olderThan: Long)

    @Insert
    suspend fun insert(alarm: Alarm): Long

    @Update
    suspend fun update(alarm: Alarm)

    @Delete
    suspend fun delete(alarm: Alarm)

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    @Query("SELECT * FROM alarms WHERE syncId = :syncId")
    suspend fun getAlarmBySyncId(syncId: String): Alarm?

    @Query("SELECT * FROM alarms WHERE updatedAt > :since")
    suspend fun getAlarmsUpdatedSince(since: Long): List<Alarm>

    @Query("SELECT * FROM alarms")
    suspend fun getAllAlarmsForBackup(): List<Alarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alarms: List<Alarm>)

    @Query("DELETE FROM alarms")
    suspend fun deleteAll()
}
