package com.apagon.rhythm.data.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

/**
 * **How an event, to-do or reminder repeats.** Stored as a small RFC 5545 RRULE subset in the
 * item's `recurrence` column (`""` = doesn't repeat):
 *
 *     FREQ=DAILY|WEEKLY|MONTHLY|YEARLY [;INTERVAL=n] [;BYDAY=MO,WE] [;UNTIL=yyyyMMdd | ;COUNT=n] [;DTSTART=yyyyMMdd]
 *
 * Android has a twin of this file (`data/model/Recurrence.kt`, java.time). **Both must give the same
 * dates** — the same test table runs on each, and the string travels between them through sync.
 *
 * Rules, matching what people expect from a phone calendar:
 * - The series starts at the item's own date. A weekly rule with days only produces those days on or
 *   after the start (a Tuesday start with BYDAY=MO,WE first falls on Wednesday).
 * - Monthly on the 29th–31st lands on the last day of shorter months; Feb 29 yearly lands on Feb 28.
 * - UNTIL is inclusive. COUNT counts produced dates, the first one included.
 * - DTSTART ([anchor]) is only set on to-dos and reminders, which move their own date forward each
 *   time: it remembers where the series began, so "monthly on the 31st" doesn't drift to the 28th after
 *   February and COUNT keeps counting. Events never move, so their start date is the anchor.
 */
data class Recurrence(
    val freq: Freq,
    val interval: Int = 1,
    /** ISO day numbers, 1 = Monday … 7 = Sunday. Weekly only; empty = the start's weekday. */
    val byDay: Set<Int> = emptySet(),
    val until: LocalDate? = null,
    val count: Int? = null,
    /** Where the series began, when that isn't the item's current date (see the class KDoc). */
    val anchor: LocalDate? = null
) {
    enum class Freq { DAILY, WEEKLY, MONTHLY, YEARLY }

    init {
        require(interval >= 1) { "interval must be at least 1" }
        require(count == null || count >= 1) { "count must be at least 1" }
        require(byDay.all { it in 1..7 }) { "byDay must be ISO day numbers" }
    }

    fun format(): String = buildString {
        append("FREQ=").append(freq.name)
        if (interval != 1) append(";INTERVAL=").append(interval)
        if (freq == Freq.WEEKLY && byDay.isNotEmpty()) append(";BYDAY=").append(byDay.sorted().joinToString(",") { CODES[it - 1] })
        until?.let { append(";UNTIL=").append(it.toString().replace("-", "")) }
        count?.let { append(";COUNT=").append(it) }
        anchor?.let { append(";DTSTART=").append(it.toString().replace("-", "")) }
    }

    /** Every date of the series, in order. Bounded to [MAX_YEARS] after the start. */
    fun dates(start: LocalDate): Sequence<LocalDate> {
        val horizon = start.plus(MAX_YEARS, DateTimeUnit.YEAR)
        val raw: Sequence<LocalDate> = when (freq) {
            Freq.DAILY -> generateSequence(0) { it + 1 }.map { start.plus(it * interval, DateTimeUnit.DAY) }
            Freq.WEEKLY -> {
                val days = (byDay.ifEmpty { setOf(start.dayOfWeek.isoDayNumber) }).sorted()
                val monday = start.minus(start.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
                generateSequence(0) { it + 1 }.flatMap { w ->
                    val weekStart = monday.plus(w * interval * 7, DateTimeUnit.DAY)
                    days.asSequence().map { weekStart.plus(it - 1, DateTimeUnit.DAY) }
                }.filter { it >= start }
            }
            Freq.MONTHLY -> generateSequence(0) { it + 1 }.map { addMonthsClamped(start, it * interval) }
            Freq.YEARLY -> generateSequence(0) { it + 1 }.map { addMonthsClamped(start, it * interval * 12) }
        }
        var s = raw.takeWhile { it <= horizon }
        until?.let { u -> s = s.takeWhile { it <= u } }
        count?.let { c -> s = s.take(c) }
        return s
    }

    /** Dates of the series that fall in [from]..[to], minus [skip]. */
    fun occurrences(start: LocalDate, from: LocalDate, to: LocalDate, skip: Set<LocalDate> = emptySet()): List<LocalDate> =
        dates(start).dropWhile { it < from }.takeWhile { it <= to }.filter { it !in skip }.toList()

    /** The first date of the series strictly after [after], or null when the series has ended. */
    fun next(start: LocalDate, after: LocalDate): LocalDate? = dates(start).firstOrNull { it > after }

    /** "Every 2 weeks on Mon, Wed, until Dec 20, 2026" — the editor row and Coqui's replies. */
    fun describe(start: LocalDate? = null): String {
        val unit = when (freq) { Freq.DAILY -> "day"; Freq.WEEKLY -> "week"; Freq.MONTHLY -> "month"; Freq.YEARLY -> "year" }
        val days = byDay.ifEmpty { start?.let { setOf(it.dayOfWeek.isoDayNumber) } ?: emptySet() }.sorted()
        val base = when {
            freq == Freq.WEEKLY && interval == 1 && days == listOf(1, 2, 3, 4, 5) -> "Every weekday"
            freq == Freq.WEEKLY && interval == 1 && days.size == 7 -> "Daily"
            interval == 1 -> when (freq) { Freq.DAILY -> "Daily"; Freq.WEEKLY -> "Weekly"; Freq.MONTHLY -> "Monthly"; Freq.YEARLY -> "Yearly" }
            else -> "Every $interval ${unit}s"
        }
        val detail = when {
            freq == Freq.WEEKLY && base != "Every weekday" && base != "Daily" && days.isNotEmpty() ->
                " on " + days.joinToString(", ") { SHORT[it - 1] }
            freq == Freq.MONTHLY && start != null -> " on day ${start.day}"
            freq == Freq.YEARLY && start != null -> " on ${MONTHS[start.month.number - 1]} ${start.day}"
            else -> ""
        }
        val end = when {
            until != null -> ", until ${MONTHS[until.month.number - 1]} ${until.day}, ${until.year}"
            count != null -> if (count == 1) ", once" else ", $count times"
            else -> ""
        }
        return base + detail + end
    }

    companion object {
        const val MAX_YEARS = 50
        private val CODES = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
        private val SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        /** null for `""` or anything that isn't a rule this app understands (it then just doesn't repeat). */
        fun parse(rule: String?): Recurrence? {
            if (rule.isNullOrBlank()) return null
            return runCatching {
                val parts = rule.trim().removePrefix("RRULE:").split(';').filter { it.isNotBlank() }.associate {
                    val (k, v) = it.split('=', limit = 2); k.trim().uppercase() to v.trim()
                }
                Recurrence(
                    freq = Freq.valueOf(parts.getValue("FREQ").uppercase()),
                    interval = parts["INTERVAL"]?.toInt() ?: 1,
                    byDay = parts["BYDAY"]?.split(',')?.filter { it.isNotBlank() }?.map { code ->
                        CODES.indexOf(code.trim().uppercase().takeLast(2)).also { require(it >= 0) } + 1
                    }?.toSet() ?: emptySet(),
                    until = parts["UNTIL"]?.take(8)?.let { LocalDate(it.take(4).toInt(), it.substring(4, 6).toInt(), it.substring(6, 8).toInt()) },
                    count = parts["COUNT"]?.toInt(),
                    anchor = parts["DTSTART"]?.take(8)?.let { LocalDate(it.take(4).toInt(), it.substring(4, 6).toInt(), it.substring(6, 8).toInt()) }
                )
            }.getOrNull()
        }

        /** Skipped dates as stored in `calendar_events.recurrenceExceptions` (comma-separated yyyy-MM-dd). */
        fun parseSkips(s: String?): Set<LocalDate> =
            s.orEmpty().split(',').mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }.toSet()

        fun formatSkips(dates: Set<LocalDate>): String = dates.sorted().joinToString(",")

        fun dayOfWeekNumber(d: DayOfWeek) = d.isoDayNumber

        private fun addMonthsClamped(start: LocalDate, months: Int): LocalDate {
            val firstOfMonth = LocalDate(start.year, start.month, 1).plus(DatePeriod(months = months))
            val length = firstOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
            return LocalDate(firstOfMonth.year, firstOfMonth.month, minOf(start.day, length))
        }
    }
}
