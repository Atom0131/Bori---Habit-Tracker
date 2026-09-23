package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.CalendarEventDao
import com.apagon.rhythm.data.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
class CalendarEventRepository constructor(private val dao: CalendarEventDao) {

    fun getAllActiveEvents(): Flow<List<CalendarEvent>> = dao.getAllActiveEvents()

    fun getEventsInRange(start: String, end: String): Flow<List<CalendarEvent>> =
        dao.getEventsInRange(start, end)

    suspend fun addEvent(event: CalendarEvent): Long = dao.insert(event)

    suspend fun updateEvent(event: CalendarEvent) = dao.update(event)

    suspend fun deleteEvent(event: CalendarEvent) = dao.update(event.copy(deletedAt = System.currentTimeMillis()))

    suspend fun hardDeleteEvent(event: CalendarEvent) = dao.delete(event)

    fun getDeletedEvents(): Flow<List<CalendarEvent>> = dao.getDeletedEvents()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        dao.purgeDeletedEvents(olderThan)
    }
}
