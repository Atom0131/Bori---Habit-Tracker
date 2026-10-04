package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.apagon.rhythm.data.model.CalendarEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {

    @Query("SELECT * FROM calendar_events WHERE isActive = 1 AND deletedAt IS NULL ORDER BY startDate ASC, startTime ASC")
    fun getAllActiveEvents(): Flow<List<CalendarEvent>>

    /**
     * Returns all active events whose date range overlaps [windowStart, windowEnd].
     * Overlap condition: startDate <= windowEnd AND endDate >= windowStart
     */
    @Query("""
        SELECT * FROM calendar_events
        WHERE isActive = 1
          AND deletedAt IS NULL
          AND startDate <= :windowEnd
          AND endDate   >= :windowStart
        ORDER BY startDate ASC, startTime ASC
    """)
    fun getEventsInRange(windowStart: String, windowEnd: String): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedEvents(): Flow<List<CalendarEvent>>

    @Query("DELETE FROM calendar_events WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeDeletedEvents(olderThan: Long)

    @Insert
    suspend fun insert(event: CalendarEvent): Long

    @Update
    suspend fun update(event: CalendarEvent)

    @Delete
    suspend fun delete(event: CalendarEvent)

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    @Query("SELECT * FROM calendar_events WHERE syncId = :syncId")
    suspend fun getEventBySyncId(syncId: String): CalendarEvent?

    @Query("SELECT * FROM calendar_events WHERE updatedAt > :since")
    suspend fun getEventsUpdatedSince(since: Long): List<CalendarEvent>

    @Query("SELECT * FROM calendar_events")
    suspend fun getAllEventsForBackup(): List<CalendarEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<CalendarEvent>)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAll()
}
