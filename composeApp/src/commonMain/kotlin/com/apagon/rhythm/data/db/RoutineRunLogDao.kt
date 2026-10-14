package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.apagon.rhythm.data.model.RoutineRunLog
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineRunLogDao {
    /** Upsert on (habitId, startedAt): the same run logged twice replaces its first record. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: RoutineRunLog)

    @Query("SELECT * FROM routine_runs WHERE habitId = :habitId ORDER BY startedAt DESC LIMIT :limit")
    fun recentForHabit(habitId: Long, limit: Int): Flow<List<RoutineRunLog>>

    @Query("SELECT * FROM routine_runs")
    suspend fun getAll(): List<RoutineRunLog>

    @Query("DELETE FROM routine_runs")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<RoutineRunLog>)
}
