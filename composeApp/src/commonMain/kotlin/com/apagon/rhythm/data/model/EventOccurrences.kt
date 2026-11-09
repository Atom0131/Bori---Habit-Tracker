package com.apagon.rhythm.data.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * **Repeating events are stored once and shown on every date they fall on.** These helpers turn a
 * series row into display copies, one per date — the same on Android (`data/model/EventOccurrences.kt`).
 */

val CalendarEvent.isRepeating: Boolean get() = recurrence.isNotBlank() && Recurrence.parse(recurrence) != null

/**
 * Every event in [from]..[to] (yyyy-MM-dd, inclusive): one-off events that overlap the window as they
 * are, and each date of a repeating series as a copy with shifted dates and [CalendarEvent.occurrenceDate]
 * set. Multi-day series keep their length. Skipped dates (`recurrenceExceptions`) are left out.
 */
fun List<CalendarEvent>.expandForRange(from: String, to: String): List<CalendarEvent> {
    val f = runCatching { LocalDate.parse(from) }.getOrNull() ?: return this
    val t = runCatching { LocalDate.parse(to) }.getOrNull() ?: return this
    return flatMap { e -> e.occurrencesIn(f, t) }
        .sortedWith(compareBy({ it.startDate }, { it.startTime ?: "" }))
}

fun CalendarEvent.occurrencesIn(from: LocalDate, to: LocalDate): List<CalendarEvent> {
    val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return listOf(this)
    val end = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: start
    val rule = Recurrence.parse(recurrence)
    if (rule == null) return if (start <= to && end >= from) listOf(this) else emptyList()
    val span = start.daysUntil(end).coerceAtLeast(0)
    return rule.occurrences(start, from.minus(span, DateTimeUnit.DAY), to, Recurrence.parseSkips(recurrenceExceptions)).map { d ->
        copy(startDate = d.toString(), endDate = d.plus(span, DateTimeUnit.DAY).toString(), occurrenceDate = d.toString())
    }
}

/** The start date of the first occurrence on or after [onOrAfter], or null when the series has ended. */
fun CalendarEvent.nextOccurrence(onOrAfter: LocalDate): LocalDate? {
    val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return null
    val rule = Recurrence.parse(recurrence) ?: return start.takeIf { it >= onOrAfter }
    val skips = Recurrence.parseSkips(recurrenceExceptions)
    return rule.dates(start).firstOrNull { it >= onOrAfter && it !in skips }
}
