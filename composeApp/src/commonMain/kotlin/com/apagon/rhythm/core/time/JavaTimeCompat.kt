package com.apagon.rhythm.core.time

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearsUntil
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * java.time-compatible surface over kotlinx-datetime so shared code keeps the
 * exact call shapes the app used on Android (LocalDate.now(), plusDays(),
 * DateTimeFormatter.ofPattern(), YearMonth, ChronoUnit, ...) on both platforms.
 * Formatting/display names are English-only, matching the app's UI language.
 */

// ── now() ────────────────────────────────────────────────────────────────────

fun LocalDate.Companion.now(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Clock.System.todayIn(zone)

fun LocalDateTime.Companion.now(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime =
    Clock.System.now().toLocalDateTime(zone)

fun LocalTime.Companion.now(zone: TimeZone = TimeZone.currentSystemDefault()): LocalTime =
    LocalDateTime.now(zone).time

// ── of() factories ───────────────────────────────────────────────────────────

fun LocalDate.Companion.of(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)
fun LocalDate.Companion.of(year: Int, month: Month, day: Int): LocalDate = LocalDate(year, month, day)
fun LocalTime.Companion.of(hour: Int, minute: Int, second: Int = 0): LocalTime = LocalTime(hour, minute, second)
fun LocalDateTime.Companion.of(date: LocalDate, time: LocalTime): LocalDateTime = LocalDateTime(date, time)
fun LocalDateTime.Companion.of(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): LocalDateTime =
    LocalDateTime(year, month, day, hour, minute, second)

// ── LocalDate arithmetic / accessors ────────────────────────────────────────

fun LocalDate.plusDays(n: Long): LocalDate = plus(n.toInt(), DateTimeUnit.DAY)
fun LocalDate.minusDays(n: Long): LocalDate = minus(n.toInt(), DateTimeUnit.DAY)
fun LocalDate.plusWeeks(n: Long): LocalDate = plus(n.toInt(), DateTimeUnit.WEEK)
fun LocalDate.minusWeeks(n: Long): LocalDate = minus(n.toInt(), DateTimeUnit.WEEK)
fun LocalDate.plusMonths(n: Long): LocalDate = plus(n.toInt(), DateTimeUnit.MONTH)
fun LocalDate.minusMonths(n: Long): LocalDate = minus(n.toInt(), DateTimeUnit.MONTH)
fun LocalDate.plusYears(n: Long): LocalDate = plus(n.toInt(), DateTimeUnit.YEAR)
fun LocalDate.minusYears(n: Long): LocalDate = minus(n.toInt(), DateTimeUnit.YEAR)

fun LocalDate.isAfter(other: LocalDate): Boolean = this > other
fun LocalDate.isBefore(other: LocalDate): Boolean = this < other
fun LocalDate.isEqual(other: LocalDate): Boolean = this == other

val LocalDate.monthValue: Int get() = month.number
fun LocalDate.lengthOfMonth(): Int {
    val start = LocalDate(year, month.number, 1)
    return start.daysUntil(start.plus(1, DateTimeUnit.MONTH))
}
fun LocalDate.withDayOfMonth(day: Int): LocalDate = LocalDate(year, month.number, day)
fun LocalDate.atStartOfDay(): LocalDateTime = LocalDateTime(this, LocalTime(0, 0))
fun LocalDate.atTime(hour: Int, minute: Int): LocalDateTime = LocalDateTime(this, LocalTime(hour, minute))
fun LocalDate.atTime(time: LocalTime): LocalDateTime = LocalDateTime(this, time)

// ── LocalDateTime accessors ──────────────────────────────────────────────────

fun LocalDateTime.toLocalDate(): LocalDate = date
fun LocalDateTime.toLocalTime(): LocalTime = time
fun LocalDateTime.isAfter(other: LocalDateTime): Boolean = this > other
fun LocalDateTime.isBefore(other: LocalDateTime): Boolean = this < other
fun LocalDateTime.plusDays(n: Long): LocalDateTime = LocalDateTime(date.plusDays(n), time)
fun LocalDateTime.minusDays(n: Long): LocalDateTime = LocalDateTime(date.minusDays(n), time)
val LocalDateTime.monthValue: Int get() = month.number

/** Pure calendar arithmetic (via UTC, which has no DST transitions). */
fun LocalDateTime.plusHours(n: Long): LocalDateTime =
    toInstant(TimeZone.UTC).plus(n, DateTimeUnit.HOUR).toLocalDateTime(TimeZone.UTC)

fun LocalDateTime.minusHours(n: Long): LocalDateTime = plusHours(-n)

fun LocalDateTime.plusMinutes(n: Long): LocalDateTime =
    toInstant(TimeZone.UTC).plus(n, DateTimeUnit.MINUTE).toLocalDateTime(TimeZone.UTC)

// ── Zones (java ZoneId/ZoneOffset call shapes → kotlinx TimeZone) ────────────

object ZoneId {
    fun systemDefault(): TimeZone = TimeZone.currentSystemDefault()
    fun of(id: String): TimeZone = TimeZone.of(id)
}

object ZoneOffset {
    val UTC: TimeZone = TimeZone.UTC
}

// ── Instant bridging ─────────────────────────────────────────────────────────

fun Instant.Companion.ofEpochMilli(ms: Long): Instant = fromEpochMilliseconds(ms)
fun Instant.Companion.ofEpochSecond(s: Long): Instant = fromEpochSeconds(s)
fun Instant.Companion.now(): Instant = Clock.System.now()
fun Instant.toEpochMilli(): Long = toEpochMilliseconds()

/** Stand-in for java.time.ZonedDateTime in ".atZone(...)" chains. */
class ZonedDateTimeCompat(val instant: Instant, val zone: TimeZone) {
    fun toInstant(): Instant = instant
    fun toLocalDate(): LocalDate = instant.toLocalDateTime(zone).date
    fun toLocalDateTime(): LocalDateTime = instant.toLocalDateTime(zone)
    fun toLocalTime(): LocalTime = instant.toLocalDateTime(zone).time
    fun toEpochMilli(): Long = instant.toEpochMilliseconds()
    fun toEpochSecond(): Long = instant.epochSeconds
}

fun Instant.atZone(zone: TimeZone): ZonedDateTimeCompat = ZonedDateTimeCompat(this, zone)
fun LocalDateTime.atZone(zone: TimeZone): ZonedDateTimeCompat = ZonedDateTimeCompat(toInstant(zone), zone)
fun LocalDate.atStartOfDay(zone: TimeZone): ZonedDateTimeCompat = ZonedDateTimeCompat(atStartOfDayIn(zone), zone)

// ── YearMonth (no kotlinx equivalent) ────────────────────────────────────────

data class YearMonth(val year: Int, val monthValue: Int) : Comparable<YearMonth> {
    init { require(monthValue in 1..12) { "Invalid month: $monthValue" } }

    val month: Month get() = Month(monthValue)

    companion object {
        fun now(zone: TimeZone = TimeZone.currentSystemDefault()): YearMonth =
            LocalDate.now(zone).let { YearMonth(it.year, it.month.number) }
        fun of(year: Int, month: Int): YearMonth = YearMonth(year, month)
        fun of(year: Int, month: Month): YearMonth = YearMonth(year, month.number)
        fun from(date: LocalDate): YearMonth = YearMonth(date.year, date.month.number)
        fun parse(text: String): YearMonth {
            val (y, m) = text.split("-")
            return YearMonth(y.toInt(), m.toInt())
        }
    }

    fun atDay(day: Int): LocalDate = LocalDate(year, monthValue, day)
    fun atEndOfMonth(): LocalDate = atDay(lengthOfMonth())
    fun lengthOfMonth(): Int = atDay(1).lengthOfMonth()
    fun plusMonths(n: Long): YearMonth = from(atDay(1).plusMonths(n))
    fun minusMonths(n: Long): YearMonth = from(atDay(1).minusMonths(n))
    fun isAfter(other: YearMonth): Boolean = this > other
    fun isBefore(other: YearMonth): Boolean = this < other

    fun format(formatter: DateTimeFormatter): String = formatter.format(atDay(1))

    override fun compareTo(other: YearMonth): Int =
        compareValuesBy(this, other, { it.year }, { it.monthValue })

    /** ISO "yyyy-MM", matching java.time.YearMonth.toString(). */
    override fun toString(): String = "$year-${monthValue.toString().padStart(2, '0')}"
}

fun LocalDate.toYearMonth(): YearMonth = YearMonth.from(this)

// ── java-style accessors on kotlinx enums ────────────────────────────────────

/** java.time Month.getValue() equivalent. */
val Month.value: Int get() = number

/** java.time DayOfWeek.getValue() equivalent (ISO: Monday=1..Sunday=7). */
val DayOfWeek.value: Int get() = isoDayNumber

fun LocalDateTime.Companion.ofInstant(instant: Instant, zone: TimeZone): LocalDateTime =
    instant.toLocalDateTime(zone)

// ── Display names (English, matching the app's UI) ───────────────────────────

enum class TextStyle { FULL, FULL_STANDALONE, SHORT, SHORT_STANDALONE, NARROW, NARROW_STANDALONE }

private val MONTH_FULL = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val DAY_FULL = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

fun Month.getDisplayName(style: TextStyle, @Suppress("UNUSED_PARAMETER") locale: Any? = null): String {
    val full = MONTH_FULL[number - 1]
    return when (style) {
        TextStyle.FULL, TextStyle.FULL_STANDALONE -> full
        TextStyle.SHORT, TextStyle.SHORT_STANDALONE -> full.take(3)
        TextStyle.NARROW, TextStyle.NARROW_STANDALONE -> full.take(1)
    }
}

fun DayOfWeek.getDisplayName(style: TextStyle, @Suppress("UNUSED_PARAMETER") locale: Any? = null): String {
    val full = DAY_FULL[isoDayNumber - 1]
    return when (style) {
        TextStyle.FULL, TextStyle.FULL_STANDALONE -> full
        TextStyle.SHORT, TextStyle.SHORT_STANDALONE -> full.take(3)
        TextStyle.NARROW, TextStyle.NARROW_STANDALONE -> full.take(1)
    }
}

// ── ChronoUnit ───────────────────────────────────────────────────────────────

enum class ChronoUnit {
    DAYS, WEEKS, MONTHS, YEARS;

    fun between(start: LocalDate, end: LocalDate): Long = when (this) {
        DAYS -> start.daysUntil(end).toLong()
        WEEKS -> (start.daysUntil(end) / 7).toLong()
        MONTHS -> start.monthsUntil(end).toLong()
        YEARS -> start.yearsUntil(end).toLong()
    }
}

// ── TemporalAdjusters ────────────────────────────────────────────────────────

fun interface TemporalAdjusterCompat {
    fun adjustInto(date: LocalDate): LocalDate
}

object TemporalAdjusters {
    fun previousOrSame(dayOfWeek: DayOfWeek) = TemporalAdjusterCompat { d ->
        val diff = (d.dayOfWeek.isoDayNumber - dayOfWeek.isoDayNumber + 7) % 7
        d.minusDays(diff.toLong())
    }

    fun nextOrSame(dayOfWeek: DayOfWeek) = TemporalAdjusterCompat { d ->
        val diff = (dayOfWeek.isoDayNumber - d.dayOfWeek.isoDayNumber + 7) % 7
        d.plusDays(diff.toLong())
    }
}

fun LocalDate.with(adjuster: TemporalAdjusterCompat): LocalDate = adjuster.adjustInto(this)

// ── DateTimeFormatter ────────────────────────────────────────────────────────

/** Thrown on parse failure; kotlinx parse errors are rethrown as this. */
class DateTimeParseException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

/**
 * Minimal pattern-based formatter covering every pattern the app uses:
 * y yyyy M MM MMM MMMM d dd E EEE EEEE H HH h mm a and 'quoted' literals.
 */
class DateTimeFormatter private constructor(private val pattern: String) {

    companion object {
        val ISO_LOCAL_DATE = DateTimeFormatter("yyyy-MM-dd")
        val ISO_LOCAL_TIME = DateTimeFormatter("HH:mm:ss")
        fun ofPattern(pattern: String): DateTimeFormatter = DateTimeFormatter(pattern)
    }

    fun format(date: LocalDate): String = render(date, null)
    fun format(dateTime: LocalDateTime): String = render(dateTime.date, dateTime.time)
    fun format(time: LocalTime): String = render(null, time)

    fun parseLocalDate(text: String): LocalDate = try {
        when (pattern) {
            "yyyy-MM-dd" -> LocalDate.parse(text)
            else -> throw IllegalArgumentException("Unsupported parse pattern: $pattern")
        }
    } catch (e: IllegalArgumentException) {
        throw DateTimeParseException("Could not parse '$text' with pattern '$pattern'", e)
    }

    fun parseLocalDateTime(text: String): LocalDateTime = try {
        when (pattern) {
            "yyyy-MM-dd HH:mm" -> {
                val (d, t) = text.split(" ")
                LocalDateTime(LocalDate.parse(d), LocalTime.parse(t))
            }
            else -> LocalDateTime.parse(text)
        }
    } catch (e: IllegalArgumentException) {
        throw DateTimeParseException("Could not parse '$text' with pattern '$pattern'", e)
    }

    private fun render(date: LocalDate?, time: LocalTime?): String {
        val sb = StringBuilder()
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i]
            when {
                c == '\'' -> {
                    val end = pattern.indexOf('\'', i + 1)
                    sb.append(pattern.substring(i + 1, end))
                    i = end + 1
                }
                c.isLetter() -> {
                    var j = i
                    while (j < pattern.length && pattern[j] == c) j++
                    sb.append(renderToken(pattern.substring(i, j), date, time))
                    i = j
                }
                else -> {
                    sb.append(c); i++
                }
            }
        }
        return sb.toString()
    }

    private fun renderToken(token: String, date: LocalDate?, time: LocalTime?): String = when (token) {
        "yyyy", "y" -> date!!.year.toString()
        "yy" -> (date!!.year % 100).toString().padStart(2, '0')
        "M" -> date!!.month.number.toString()
        "MM" -> date!!.month.number.toString().padStart(2, '0')
        "MMM" -> date!!.month.getDisplayName(TextStyle.SHORT)
        "MMMM" -> date!!.month.getDisplayName(TextStyle.FULL)
        "d" -> date!!.day.toString()
        "dd" -> date!!.day.toString().padStart(2, '0')
        "E", "EE", "EEE" -> date!!.dayOfWeek.getDisplayName(TextStyle.SHORT)
        "EEEE" -> date!!.dayOfWeek.getDisplayName(TextStyle.FULL)
        "H" -> time!!.hour.toString()
        "HH" -> time!!.hour.toString().padStart(2, '0')
        "h" -> ((time!!.hour + 11) % 12 + 1).toString()
        "hh" -> ((time!!.hour + 11) % 12 + 1).toString().padStart(2, '0')
        "m" -> time!!.minute.toString()
        "mm" -> time!!.minute.toString().padStart(2, '0')
        "s" -> time!!.second.toString()
        "ss" -> time!!.second.toString().padStart(2, '0')
        "a" -> if (time!!.hour < 12) "AM" else "PM"
        else -> throw IllegalArgumentException("Unsupported pattern token: $token")
    }
}

fun LocalDate.format(formatter: DateTimeFormatter): String = formatter.format(this)
fun LocalDateTime.format(formatter: DateTimeFormatter): String = formatter.format(this)
fun LocalTime.format(formatter: DateTimeFormatter): String = formatter.format(this)

fun LocalDate.Companion.parse(text: String, formatter: DateTimeFormatter): LocalDate =
    formatter.parseLocalDate(text)

fun LocalDateTime.Companion.parse(text: String, formatter: DateTimeFormatter): LocalDateTime =
    formatter.parseLocalDateTime(text)
