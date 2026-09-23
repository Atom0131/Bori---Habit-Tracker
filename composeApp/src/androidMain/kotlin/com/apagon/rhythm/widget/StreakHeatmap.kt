package com.apagon.rhythm.widget
import com.apagon.rhythm.core.time.*

import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.ui.util.isScheduledForDate
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.YearMonth
import com.apagon.rhythm.core.time.ZoneId

internal enum class DayState { DONE, MISSED, NOT_SCHEDULED, NO_DATA }

/**
 * Current-month calendar grid for [habit], Sunday-first (matches the rest of the app's
 * day-of-week convention — see [[JournalWeekStrip]]/`Extensions.kt` DAY_NAMES). Leading `null`
 * entries pad the list out to [today]'s month's first day's real weekday so a caller chunking
 * this into rows of 7 gets a grid that lines up like an actual calendar; every other entry is a
 * real day of the month with its [DayState].
 */
internal fun computeCurrentMonthCalendar(
    habit: Habit,
    completions: List<HabitCompletion>,
    today: LocalDate
): List<DayState?> {
    val createdDate = Instant.ofEpochMilli(habit.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val doneDates = completions.map { LocalDate.parse(it.dateCompleted) }.toHashSet()
    val yearMonth = YearMonth.from(today)
    val firstDay = yearMonth.atDay(1)
    val leadingBlanks = firstDay.dayOfWeek.value % 7 // Sun=0 .. Sat=6, matches the app's Sunday-first convention

    val days = (1..yearMonth.lengthOfMonth()).map { dayOfMonth ->
        val date = yearMonth.atDay(dayOfMonth)
        when {
            date.isAfter(today) || date.isBefore(createdDate) -> DayState.NO_DATA
            !habit.isScheduledForDate(date) -> DayState.NOT_SCHEDULED
            date in doneDates -> DayState.DONE
            else -> DayState.MISSED
        }
    }
    return List<DayState?>(leadingBlanks) { null } + days
}
