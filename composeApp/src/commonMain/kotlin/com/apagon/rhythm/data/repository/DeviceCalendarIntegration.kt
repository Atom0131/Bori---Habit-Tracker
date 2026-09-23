package com.apagon.rhythm.data.repository

import com.apagon.rhythm.data.model.CalendarEvent
import kotlinx.datetime.LocalDate

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int
)

/**
 * Read/write access to the OS calendar store.
 * Android actual: CalendarContract (CalendarIntegrationRepository).
 * iOS: EventKit eventually; stubbed (empty/no-op) in the first release —
 * device-calendar sync is a deferred feature there.
 */
interface DeviceCalendarIntegration {
    fun fetchAvailableCalendars(): List<DeviceCalendar>
    fun fetchEventsForDate(date: LocalDate, enabledIds: Set<String> = emptySet()): List<CalendarEvent>
    fun fetchEventDatesInRange(start: LocalDate, end: LocalDate, enabledIds: Set<String> = emptySet()): Set<String>
    fun insertEventToNativeCalendar(event: CalendarEvent, targetCalendarId: Long): Long?
    fun updateEventInNativeCalendar(event: CalendarEvent): Boolean
    fun deleteEventFromNativeCalendar(eventId: Long): Boolean
}
