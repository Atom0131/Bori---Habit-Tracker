package com.apagon.rhythm.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import com.apagon.rhythm.ui.util.isScheduledForDate
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * Desktop port of the real Stats content from androidMain's
 * ui/stats/StatsScreen.kt (HabitProgressCard/StatsHabitSheet/
 * computeProgressStats) — that screen had no desktop consumer (StatsScreen()
 * there is just `= JournalScreen()`), so Stage 9 builds a minimal desktop
 * habit-detail surface to host it (see ui/habit/DesktopHabitDetailScreen.kt).
 * computeProgressStats' body is copied verbatim (pure logic, already
 * portable); HabitProgressCard/StatsHabitSheet are reimplemented without
 * Icons.Default.Edit or Habit.resolvedIcon() (both material-icons-extended,
 * androidMain-only) and without HabitCard (already ruled non-portable in
 * Stage 8) — a plain Surface stands in for both.
 */
internal data class ProgressStats(
    val completedPeriods: Int,
    val totalPeriods: Int,
    val periodLabel: String
)

internal fun computeProgressStats(habit: Habit, completedDates: Set<String>, today: LocalDate): ProgressStats {
    val yearStart = LocalDate.of(today.year, 1, 1)
    val rawCreated = Instant.ofEpochMilli(habit.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val createdDate = if (rawCreated.isBefore(yearStart)) yearStart else rawCreated

    return when (habit.frequency) {
        HabitFrequency.DAILY -> {
            val yearEnd = LocalDate.of(today.year, 12, 31)
            val goalEnd = if (habit.durationDays > 0)
                rawCreated.plusDays(habit.durationDays.toLong() - 1).coerceAtMost(yearEnd)
            else yearEnd
            val start = if (createdDate.isAfter(yearStart)) createdDate else yearStart
            var scheduled = 0
            var completed = 0
            var d = start
            while (!d.isAfter(goalEnd)) {
                if (habit.isScheduledForDate(d)) {
                    scheduled++
                    if (!d.isAfter(today) && d.format(ISO_LOCAL_DATE) in completedDates) completed++
                }
                d = d.plusDays(1)
            }
            ProgressStats(completed, scheduled, "days")
        }
        HabitFrequency.WEEKLY -> {
            val createdWeekIndex = (createdDate.dayOfYear - 1) / 7
            val todayWeekIndex = (today.dayOfYear - 1) / 7
            val yearEndWeekIndex = 51
            var completed = 0
            val total = if (habit.durationDays > 0) (habit.durationDays + 6) / 7
            else (yearEndWeekIndex - createdWeekIndex + 1).coerceAtLeast(0)
            for (w in createdWeekIndex..todayWeekIndex) {
                val wStart = yearStart.plusDays((w * 7).toLong())
                val wEnd = wStart.plusDays(6)
                if (completedDates.any { dateStr ->
                        val d = LocalDate.parse(dateStr)
                        !d.isBefore(wStart) && !d.isAfter(wEnd)
                    }) completed++
            }
            ProgressStats(completed, total, "weeks")
        }
        HabitFrequency.MONTHLY -> {
            val startMonth = YearMonth.from(createdDate)
            val endMonth = YearMonth.from(today)
            val total = run {
                var m = startMonth; var count = 0
                val goalEnd = if (habit.durationDays > 0) rawCreated.plusDays(habit.durationDays.toLong() - 1) else today
                while (!m.isAfter(YearMonth.from(goalEnd))) { count++; m = m.plusMonths(1) }
                count
            }
            var completed = 0; var m = startMonth
            while (!m.isAfter(endMonth)) {
                if (completedDates.any { YearMonth.from(LocalDate.parse(it)) == m }) completed++
                m = m.plusMonths(1)
            }
            ProgressStats(completed, total, "months")
        }
    }
}

@Composable
internal fun DesktopHabitStatsContent(stat: HabitYearStats) {
    val habitColor = remember(stat.habit.colorIndex, stat.habit.colorArgb) {
        resolveDisplayColor(stat.habit.colorIndex, stat.habit.colorArgb)
    }
    val today = remember { LocalDate.now() }
    val progressStats = remember(stat.habit, stat.completedDates) {
        computeProgressStats(stat.habit, stat.completedDates, today)
    }
    val pct = if (progressStats.totalPeriods > 0) progressStats.completedPeriods * 100 / progressStats.totalPeriods else 0
    val fillFraction = if (progressStats.totalPeriods > 0)
        progressStats.completedPeriods.toFloat() / progressStats.totalPeriods else 0f

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stat.habit.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = habitColor
        )
        if (stat.habit.description.isNotBlank()) {
            Text(text = stat.habit.description, style = MaterialTheme.typography.bodyMedium, color = habitColor.copy(alpha = 0.7f))
        }

        HorizontalDivider()
        DesktopDetailField(label = "Schedule", value = scheduleLabel(stat.habit))

        if (stat.habit.durationDays > 0) {
            val daysSinceStart = remember(stat.habit.createdAt) {
                ChronoUnit.DAYS.between(
                    Instant.ofEpochMilli(stat.habit.createdAt).atZone(ZoneId.systemDefault()).toLocalDate(),
                    today
                ).toInt()
            }
            val daysLeft = (stat.habit.durationDays - daysSinceStart).coerceAtLeast(0)
            DesktopDetailField(label = "Goal", value = "$daysLeft days left (${stat.habit.durationDays} day goal)")
        } else {
            DesktopDetailField(
                label = "This year",
                value = "${progressStats.completedPeriods} of ${progressStats.totalPeriods} ${progressStats.periodLabel} completed ($pct%)"
            )
        }

        HorizontalDivider()
        Spacer(Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { fillFraction },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
            color = habitColor,
            trackColor = habitColor.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun DesktopDetailField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

// Local copy of androidMain's ui/util/Extensions.kt Habit.scheduleLabel() —
// that file also pulls Habit.resolvedIcon() (material-icons-extended,
// androidMain-only), so duplicating this one small function here avoids
// depending on a file that can't fully move to commonMain as-is.
private val DAY_NAMES_SHORT = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

private fun Int.ordinalSuffix(): String = when {
    this in 11..13 -> "${this}th"
    this % 10 == 1 -> "${this}st"
    this % 10 == 2 -> "${this}nd"
    this % 10 == 3 -> "${this}rd"
    else -> "${this}th"
}

private fun scheduleLabel(habit: Habit): String = when (habit.frequency) {
    HabitFrequency.DAILY -> "Every day"
    HabitFrequency.WEEKLY -> {
        val days = (0..6).filter { (habit.weekDaysMask and (1 shl it)) != 0 }
        if (days.isNotEmpty()) "Weekly on ${days.joinToString(", ") { DAY_NAMES_SHORT[it] }}"
        else "Weekly (${habit.targetDaysPerWeek}× per week)"
    }
    HabitFrequency.MONTHLY -> {
        val days = (1..31).filter { (habit.monthDaysMask and (1 shl (it - 1))) != 0 }
        if (days.isNotEmpty()) "Monthly on the ${days.joinToString(", ") { it.ordinalSuffix() }}"
        else "Monthly (${habit.targetDaysPerMonth}× per month)"
    }
}
