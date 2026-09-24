package com.apagon.rhythm.data.repository

import com.apagon.rhythm.data.model.CalendarEvent
import kotlinx.datetime.LocalDate

// Desktop actual for DeviceCalendarIntegration. No Linux desktop equivalent
// of Android's CalendarContract exists — matches the interface's own doc
// comment anticipating an "empty/no-op" stub (as planned for iOS). Real
// CalDAV/ICS sync against system calendars is deferred, separate work; this
// keeps Calendar screens fully functional on in-app (DB-backed) events only.
class DesktopDeviceCalendarIntegration : DeviceCalendarIntegration {
    override fun fetchAvailableCalendars(): List<DeviceCalendar> = emptyList()

    override fun fetchEventsForDate(date: LocalDate, enabledIds: Set<String>): List<CalendarEvent> = emptyList()

    override fun fetchEventDatesInRange(start: LocalDate, end: LocalDate, enabledIds: Set<String>): Set<String> = emptySet()

    override fun insertEventToNativeCalendar(event: CalendarEvent, targetCalendarId: Long): Long? = null

    override fun updateEventInNativeCalendar(event: CalendarEvent): Boolean = false

    override fun deleteEventFromNativeCalendar(eventId: Long): Boolean = false
}
