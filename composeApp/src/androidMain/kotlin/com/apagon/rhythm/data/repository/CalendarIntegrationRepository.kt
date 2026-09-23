package com.apagon.rhythm.data.repository
import com.apagon.rhythm.core.time.*

import android.content.Context
import android.provider.CalendarContract
import com.apagon.rhythm.data.model.CalendarEvent
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import com.apagon.rhythm.core.time.ZoneId
import com.apagon.rhythm.core.time.ZoneOffset
import com.apagon.rhythm.core.time.DateTimeFormatter

class CalendarIntegrationRepository(
    private val context: Context
) : DeviceCalendarIntegration {
    /**
     * Fetches all available calendars synced to the device.
     */
    override fun fetchAvailableCalendars(): List<DeviceCalendar> {
        val calendars = mutableListOf<DeviceCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.ACCOUNT_NAME} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIdx = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val colorIdx = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)

                while (it.moveToNext()) {
                    calendars.add(
                        DeviceCalendar(
                            id = it.getLong(idIdx),
                            displayName = it.getString(nameIdx) ?: "Unknown",
                            accountName = it.getString(accountIdx) ?: "Unknown",
                            color = it.getInt(colorIdx)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return calendars
    }

    /**
     * Fetches events from the system Calendar Provider for a given date.
     * @param enabledIds If not empty, only events from these calendar IDs will be returned.
     */
    override fun fetchEventsForDate(date: LocalDate, enabledIds: Set<String>): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.OWNER_ACCOUNT,
            CalendarContract.Instances.CALENDAR_ID
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, startOfDay)
        android.content.ContentUris.appendId(builder, endOfDay)

        // Filter by specific calendar IDs if provided
        val selection = if (enabledIds.isNotEmpty()) {
            "${CalendarContract.Instances.CALENDAR_ID} IN (${enabledIds.joinToString(",") { "?" }})"
        } else null
        
        val selectionArgs = if (enabledIds.isNotEmpty()) {
            enabledIds.toTypedArray()
        } else null

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val noteIdx = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val startIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val colorIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_COLOR)
                val calColorIdx = it.getColumnIndex(CalendarContract.Instances.CALENDAR_COLOR)
                val calNameIdx = it.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
                val accountIdx = it.getColumnIndex(CalendarContract.Instances.OWNER_ACCOUNT)

                while (it.moveToNext()) {
                    val title = if (titleIdx >= 0) it.getString(titleIdx) ?: "No Title" else "No Title"
                    val note = if (noteIdx >= 0) it.getString(noteIdx) ?: "" else ""
                    val startMillis = if (startIdx >= 0) it.getLong(startIdx) else startOfDay
                    val endMillis = if (endIdx >= 0) it.getLong(endIdx) else endOfDay
                    val allDay = if (allDayIdx >= 0) it.getInt(allDayIdx) == 1 else false
                    
                    val color = if (colorIdx >= 0 && !it.isNull(colorIdx)) {
                        it.getInt(colorIdx)
                    } else if (calColorIdx >= 0 && !it.isNull(calColorIdx)) {
                        it.getInt(calColorIdx)
                    } else {
                        null
                    }

                    // All-day events are stored as UTC midnight in Android Calendar; use UTC to avoid day shift
                    val effectiveZone: kotlinx.datetime.TimeZone = if (allDay) ZoneOffset.UTC else zone
                    val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMillis), effectiveZone)
                    val endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endMillis), effectiveZone)

                    events.add(
                        CalendarEvent(
                            id = if (idIdx >= 0) -it.getLong(idIdx) else -1L,
                            title = title,
                            note = note,
                            startDate = startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            endDate = endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            startTime = if (allDay) null else startDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            endTime = if (allDay) null else endDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            colorArgb = color,
                            calendarDisplayName = if (calNameIdx >= 0) it.getString(calNameIdx) else null,
                            accountName = if (accountIdx >= 0) it.getString(accountIdx) else null,
                            isActive = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return events
    }

    /**
     * Fetches all event dates in a range for calendar indicators.
     */
    override fun fetchEventDatesInRange(start: LocalDate, end: LocalDate, enabledIds: Set<String>): Set<String> {
        val dates = mutableSetOf<String>()
        val startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.ALL_DAY
        )
        
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, startMillis)
        android.content.ContentUris.appendId(builder, endMillis)

        val selection = if (enabledIds.isNotEmpty()) {
            "${CalendarContract.Instances.CALENDAR_ID} IN (${enabledIds.joinToString(",") { "?" }})"
        } else null
        
        val selectionArgs = if (enabledIds.isNotEmpty()) {
            enabledIds.toTypedArray()
        } else null

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.use {
                val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val allDayIdx = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val localZone = ZoneId.systemDefault()
                while (it.moveToNext()) {
                    val millis = it.getLong(beginIdx)
                    val isAllDay = allDayIdx >= 0 && it.getInt(allDayIdx) == 1
                    // All-day events are stored as UTC midnight; use UTC to avoid day shift
                    val effectiveZone: kotlinx.datetime.TimeZone = if (isAllDay) ZoneOffset.UTC else localZone
                    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), effectiveZone).toLocalDate()
                    dates.add(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dates
    }

    /**
     * Inserts a new event into the device's native calendar.
     */
    override fun insertEventToNativeCalendar(event: CalendarEvent, targetCalendarId: Long): Long? {
        val values = android.content.ContentValues().apply {
            put(CalendarContract.Events.DTSTART, parseToMillis(event.startDate, event.startTime))
            put(CalendarContract.Events.DTEND, parseToMillis(event.endDate, event.endTime ?: event.startTime))
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.note)
            put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            if (event.startTime == null) {
                put(CalendarContract.Events.ALL_DAY, 1)
            }
        }

        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Updates an existing event in the device's native calendar.
     */
    override fun updateEventInNativeCalendar(event: CalendarEvent): Boolean {
        if (event.id >= 0) return false // Ensure it's a native event ID (which we negated on read)
        val nativeId = -event.id
        val values = android.content.ContentValues().apply {
            put(CalendarContract.Events.DTSTART, parseToMillis(event.startDate, event.startTime))
            put(CalendarContract.Events.DTEND, parseToMillis(event.endDate, event.endTime ?: event.startTime))
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.note)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            if (event.startTime == null) {
                put(CalendarContract.Events.ALL_DAY, 1)
            } else {
                put(CalendarContract.Events.ALL_DAY, 0)
            }
        }

        val updateUri = android.content.ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, nativeId)
        return try {
            val rows = context.contentResolver.update(updateUri, values, null, null)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Deletes an event from the device's native calendar.
     */
    override fun deleteEventFromNativeCalendar(eventId: Long): Boolean {
        if (eventId >= 0) return false // Ensure it's a native event ID
        val nativeId = -eventId
        val deleteUri = android.content.ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, nativeId)
        return try {
            val rows = context.contentResolver.delete(deleteUri, null, null)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun parseToMillis(dateStr: String, timeStr: String?): Long {
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        return if (timeStr != null) {
            val time = kotlinx.datetime.LocalTime.parse(timeStr)
            date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
}
