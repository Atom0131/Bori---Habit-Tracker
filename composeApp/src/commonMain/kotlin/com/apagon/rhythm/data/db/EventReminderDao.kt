package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.apagon.rhythm.data.model.EventReminder

// Backup/restore only, this pass — desktop has no event-reminder scheduling UI yet.
@Dao
interface EventReminderDao {
    @Query("SELECT * FROM event_reminders")
    suspend fun getAllForBackup(): List<EventReminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<EventReminder>)

    @Query("DELETE FROM event_reminders")
    suspend fun deleteAll()

    // ── Sync (Stage 2) — full-replace-per-parent ────────────────────────────

    @Query("SELECT * FROM event_reminders WHERE eventId = :eventId")
    suspend fun getForEventOnce(eventId: Long): List<EventReminder>

    @Query("DELETE FROM event_reminders WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: Long)

    // Desktop has no event-reminder scheduling UI yet (see the top-of-file comment). The moment
    // one is built, its mutator must call touchCalendarEvent after mutating a reminder row, or the
    // edit will silently never sync — reminders only sync as part of their parent event's full
    // current child list, sent only when the parent itself qualifies via getEventsUpdatedSince.
    @Query("UPDATE calendar_events SET updatedAt = :updatedAt WHERE id = :eventId")
    suspend fun touchCalendarEvent(eventId: Long, updatedAt: Long)
}
