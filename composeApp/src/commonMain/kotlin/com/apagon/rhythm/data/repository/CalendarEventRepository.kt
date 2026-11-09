package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.CalendarEventDao
import com.apagon.rhythm.data.db.EventReminderDao
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.expandForRange
import com.apagon.rhythm.data.model.isRepeating
import com.apagon.rhythm.data.model.Recurrence
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import com.apagon.rhythm.data.model.EventReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
class CalendarEventRepository constructor(
    private val dao: CalendarEventDao,
    private val eventReminderDao: EventReminderDao
) {

    fun getAllActiveEvents(): Flow<List<CalendarEvent>> = dao.getAllActiveEvents()

    fun getEventsInRange(start: String, end: String): Flow<List<CalendarEvent>> =
        // Repeating series come back once from the query; expanded here to one copy per date.
        dao.getEventsInRange(start, end).map { it.expandForRange(start, end) }

    suspend fun addEvent(event: CalendarEvent): Long = dao.insert(event)

    suspend fun updateEvent(event: CalendarEvent) = dao.update(event.copy(updatedAt = System.currentTimeMillis()))

    /**
     * **Saves an edit from the event editor.** The editor builds a fresh event, so the stored row is the
     * base and only the editor's own fields are taken from [edited] — syncId, createdAt and skipped
     * dates survive. For one date of a repeating series (`edited.occurrenceDate` set):
     * - [onlyThis]: that date is skipped in the series and becomes an event of its own (returned);
     * - otherwise the whole series changes, moved by however far that date was moved.
     * Returns the event whose alerts should be re-armed.
     */
    suspend fun saveEdit(edited: CalendarEvent, onlyThis: Boolean): CalendarEvent {
        val stored = dao.getEventByIdRaw(edited.id) ?: return edited.also { updateEvent(it) }
        fun applyTo(base: CalendarEvent, start: String, end: String) = base.copy(
            title = edited.title, note = edited.note, startDate = start, endDate = end,
            startTime = edited.startTime, endTime = edited.endTime, colorIndex = edited.colorIndex,
            colorArgb = edited.colorArgb, recurrence = edited.recurrence, occurrenceDate = null
        )
        val occ = edited.occurrenceDate
        if (occ == null || !stored.isRepeating) {
            return applyTo(stored, edited.startDate, edited.endDate).also { updateEvent(it) }
        }
        if (onlyThis) {
            updateEvent(stored.copy(recurrenceExceptions = withSkip(stored.recurrenceExceptions, occ)))
            val single = applyTo(stored, edited.startDate, edited.endDate).copy(
                id = 0, recurrence = "", recurrenceExceptions = "", syncId = newSyncId(),
                createdAt = System.currentTimeMillis()
            )
            return single.copy(id = addEvent(single))
        }
        val (start, end) = shiftSeries(stored.startDate, occ, edited.startDate, edited.endDate)
        return applyTo(stored, start, end).also { updateEvent(it) }
    }

    /** Deletes one date of a repeating series ([onlyThis], by skipping it) or the whole event. */
    suspend fun deleteFromEditor(event: CalendarEvent, onlyThis: Boolean) {
        val stored = dao.getEventByIdRaw(event.id) ?: event
        val occ = event.occurrenceDate
        if (onlyThis && occ != null && stored.isRepeating) {
            updateEvent(stored.copy(recurrenceExceptions = withSkip(stored.recurrenceExceptions, occ)))
        } else deleteEvent(stored.copy(occurrenceDate = null))
    }

    private fun withSkip(skips: String, date: String): String =
        Recurrence.formatSkips(Recurrence.parseSkips(skips) + LocalDate.parse(date))

    /** The series' new start and end: its start moved by as many days as [occurrence] moved to [newStart]. */
    private fun shiftSeries(seriesStart: String, occurrence: String, newStart: String, newEnd: String): Pair<String, String> {
        val delta = LocalDate.parse(occurrence).daysUntil(LocalDate.parse(newStart))
        val span = LocalDate.parse(newStart).daysUntil(LocalDate.parse(newEnd)).coerceAtLeast(0)
        val start = LocalDate.parse(seriesStart).plus(delta, DateTimeUnit.DAY)
        return start.toString() to start.plus(span, DateTimeUnit.DAY).toString()
    }

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    private fun newSyncId() = kotlin.uuid.Uuid.random().toString()

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
