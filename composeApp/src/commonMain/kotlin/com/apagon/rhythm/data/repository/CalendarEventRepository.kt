package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.CalendarEventDao
import com.apagon.rhythm.data.db.EventReminderDao
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.EventReminder
import kotlinx.coroutines.flow.Flow
class CalendarEventRepository constructor(
    private val dao: CalendarEventDao,
    private val eventReminderDao: EventReminderDao
) {

    fun getAllActiveEvents(): Flow<List<CalendarEvent>> = dao.getAllActiveEvents()

    fun getEventsInRange(start: String, end: String): Flow<List<CalendarEvent>> =
        dao.getEventsInRange(start, end)

    suspend fun addEvent(event: CalendarEvent): Long = dao.insert(event)

    suspend fun updateEvent(event: CalendarEvent) = dao.update(event.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteEvent(event: CalendarEvent) {
        val now = System.currentTimeMillis()
        dao.update(event.copy(deletedAt = now, updatedAt = now))
    }

    suspend fun hardDeleteEvent(event: CalendarEvent) = dao.delete(event)

    fun getDeletedEvents(): Flow<List<CalendarEvent>> = dao.getDeletedEvents()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        dao.purgeDeletedEvents(olderThan)
    }

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    suspend fun getEventBySyncId(syncId: String): CalendarEvent? = dao.getEventBySyncId(syncId)

    suspend fun getEventsUpdatedSince(since: Long): List<CalendarEvent> = dao.getEventsUpdatedSince(since)

    suspend fun insertEventFromSync(event: CalendarEvent): Long = dao.insert(event)

    suspend fun updateEventFromSync(event: CalendarEvent) = dao.update(event)

    // ── Sync (Stage 2) — event reminders, full-replace-per-parent ───────────

    suspend fun getRemindersForEventSync(eventId: Long): List<EventReminder> = eventReminderDao.getForEventOnce(eventId)

    suspend fun replaceRemindersForEventFromSync(eventId: Long, reminders: List<EventReminder>) {
        eventReminderDao.deleteForEvent(eventId)
        if (reminders.isNotEmpty()) {
            eventReminderDao.insertAll(reminders.map { it.copy(id = 0, eventId = eventId) })
        }
    }
}
