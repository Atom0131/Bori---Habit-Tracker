package com.apagon.rhythm.data.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * **Repeating to-dos and reminders move forward instead of finishing.** Same rules on Android
 * (`data/model/RepeatRoll.kt`); the repositories call these when one is completed (or a reminder rings).
 */

val Todo.isRepeating: Boolean get() = Recurrence.parse(recurrence) != null && dueDate.length >= 10
val Reminder.isRepeating: Boolean get() = Recurrence.parse(recurrence) != null

/**
 * The to-do as it should be after being checked off: due on the next date of its series that is after
 * both its current due date and [today] (so an overdue daily to-do comes back tomorrow, not three times
 * today), same time of day, open again. Null when the series has ended — then it just completes.
 */
fun Todo.nextRepeat(today: LocalDate): Todo? {
    val rule = Recurrence.parse(recurrence) ?: return null
    val due = runCatching { LocalDate.parse(dueDate.take(10)) }.getOrNull() ?: return null
    val anchor = rule.anchor ?: due
    val after = maxOf(due, today)
    val next = rule.dates(anchor).firstOrNull { it > after } ?: return null
    return copy(dueDate = next.toString() + dueDate.drop(10), recurrence = rule.copy(anchor = anchor).format(),
        isCompleted = false, completedAt = null)
}

/**
 * The reminder moved to its next date that is after its current one and still ahead of [now] (missed
 * dates are skipped), same time of day, open again. Null when the series has ended.
 */
fun Reminder.nextRepeat(now: LocalDateTime): Reminder? {
    val rule = Recurrence.parse(recurrence) ?: return null
    val date = runCatching { LocalDate.parse(dateTime.take(10)) }.getOrNull() ?: return null
    val time = runCatching { LocalTime.parse(dateTime.substring(11, 16)) }.getOrNull() ?: return null
    val anchor = rule.anchor ?: date
    val next = rule.dates(anchor).firstOrNull { it > date && LocalDateTime(it, time) > now } ?: return null
    return copy(dateTime = "$next ${dateTime.substring(11, 16)}", recurrence = rule.copy(anchor = anchor).format(),
        isCompleted = false, completedAt = null)
}

/**
 * A repeating reminder stays on its date after it rings (overdue until ticked done). When a later date
 * of its series has come due too, it moves to the latest one that is due at [now] (the ones in between
 * were missed), so it rings for that one. Null when its current date is still the latest due one.
 * Same on Android.
 */
fun Reminder.caughtUp(now: LocalDateTime): Reminder? {
    val rule = Recurrence.parse(recurrence) ?: return null
    val date = runCatching { LocalDate.parse(dateTime.take(10)) }.getOrNull() ?: return null
    val time = runCatching { LocalTime.parse(dateTime.substring(11, 16)) }.getOrNull() ?: return null
    val anchor = rule.anchor ?: date
    val latest = rule.dates(anchor).takeWhile { LocalDateTime(it, time) <= now }.lastOrNull() ?: return null
    if (latest <= date) return null
    return copy(dateTime = "$latest ${dateTime.substring(11, 16)}", recurrence = rule.copy(anchor = anchor).format())
}
