package com.apagon.rhythm.ui.stats
import com.apagon.rhythm.core.time.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.ui.habit.AddHabitSheet
import com.apagon.rhythm.ui.habit.HabitListViewModel
import com.apagon.rhythm.ui.journal.JournalScreen
import com.apagon.rhythm.ui.util.DetailField
import com.apagon.rhythm.ui.util.HabitCard
import com.apagon.rhythm.ui.util.EditorialTitle
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import com.apagon.rhythm.ui.util.resolvedIcon
import com.apagon.rhythm.ui.util.isScheduledForDate
import com.apagon.rhythm.ui.util.scheduleLabel
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.YearMonth
import com.apagon.rhythm.core.time.ZoneId
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE
import com.apagon.rhythm.core.time.ChronoUnit

// ---------------------------------------------------------------------------
// Progress stats (frequency-aware, excludes pre-creation periods)
// ---------------------------------------------------------------------------

// ProgressStats/computeProgressStats now live in commonMain
// (ui/stats/DesktopStatsContent.kt, Stage 10's desktop port) — internal
// visibility, same module, same package, so this file uses them directly
// with no import needed. Deleted from here to fix a top-level redeclaration
// conflict caught when compileDebugKotlinAndroid was finally run after
// Stage 10 shipped.

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun StatsScreen(onNavigateToSettings: () -> Unit = {}) {
    JournalScreen(onNavigateToSettings = onNavigateToSettings)
}

// ---------------------------------------------------------------------------
// Progress card (mockup design)
// ---------------------------------------------------------------------------

@Composable
internal fun HabitProgressCard(
    habitStat: HabitYearStats,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val habitColor = remember(habitStat.habit.colorIndex, habitStat.habit.colorArgb) {
        resolveDisplayColor(habitStat.habit.colorIndex, habitStat.habit.colorArgb)
    }
    val categoryIcon = remember(habitStat.habit.colorIndex, habitStat.habit.iconIndex) {
        habitStat.habit.resolvedIcon()
    }
    val today = remember { LocalDate.now() }
    val progressStats = remember(habitStat.habit, habitStat.completedDates) {
        computeProgressStats(habitStat.habit, habitStat.completedDates, today)
    }
    val pct = if (progressStats.totalPeriods > 0)
        (progressStats.completedPeriods * 100f / progressStats.totalPeriods).toInt() else 0
    val fillFraction = if (progressStats.totalPeriods > 0)
        progressStats.completedPeriods.toFloat() / progressStats.totalPeriods else 0f

    HabitCard(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(habitColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = habitColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = habitStat.habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = habitColor
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fillFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = habitColor,
                    trackColor = habitColor.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${progressStats.completedPeriods} of ${progressStats.totalPeriods} ${progressStats.periodLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Detail sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsHabitSheet(stat: HabitYearStats, onDismiss: () -> Unit, onEdit: (() -> Unit)? = null) {
    val habitColor = remember(stat.habit.colorIndex, stat.habit.colorArgb) {
        resolveDisplayColor(stat.habit.colorIndex, stat.habit.colorArgb)
    }
    val today = remember { LocalDate.now() }

    val progressStats = remember(stat.habit, stat.completedDates) {
        computeProgressStats(stat.habit, stat.completedDates, today)
    }
    val pct = if (progressStats.totalPeriods > 0)
        progressStats.completedPeriods * 100 / progressStats.totalPeriods else 0
    val fillFraction = if (progressStats.totalPeriods > 0)
        progressStats.completedPeriods.toFloat() / progressStats.totalPeriods else 0f

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stat.habit.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = habitColor,
                modifier = Modifier.fillMaxWidth()
            )
            if (stat.habit.description.isNotBlank()) {
                Text(
                    text = stat.habit.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = habitColor.copy(alpha = 0.7f)
                )
            }

            HorizontalDivider()

            DetailField(label = "Schedule", value = stat.habit.scheduleLabel())

            if (stat.habit.durationDays > 0) {
                val daysSinceStart = remember(stat.habit.createdAt) {
                    ChronoUnit.DAYS.between(
                        Instant.ofEpochMilli(stat.habit.createdAt)
                            .atZone(ZoneId.systemDefault()).toLocalDate(),
                        today
                    ).toInt()
                }
                val daysLeft = (stat.habit.durationDays - daysSinceStart).coerceAtLeast(0)
                DetailField(
                    label = "Goal",
                    value = "$daysLeft days left (${stat.habit.durationDays} day goal)"
                )
            } else {
                DetailField(
                    label = "This year",
                    value = "${progressStats.completedPeriods} of ${progressStats.totalPeriods} " +
                            "${progressStats.periodLabel} completed ($pct%)"
                )
            }

            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { fillFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = habitColor,
                trackColor = habitColor.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )

            if (onEdit != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = habitColor)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Habit")
                }
            }
        }
    }
}
