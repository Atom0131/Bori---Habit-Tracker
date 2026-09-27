package com.apagon.rhythm.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalFabContainerColor
import com.apagon.rhythm.ui.components.crystalFabContentColor
import com.apagon.rhythm.ui.components.crystalFabElevation
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stage 8's desktop Calendar tab: month grid plus a day-detail pane of
 * in-app calendar events only (no Reminders section, no habit list/checklist
 * toggling — see DesktopCalendarViewModel's doc comment and
 * ref_notes/plan_2026-09-24_calendar_port.md for why those are dropped).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopCalendarScreen(viewModel: DesktopCalendarViewModel = koinViewModel()) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedDayState = viewModel.selectedDay.collectAsState()
    val selectedDay by selectedDayState
    val monthIndicatorDates by viewModel.monthIndicatorDates.collectAsState()
    val selectedDayEvents by viewModel.selectedDayEvents.collectAsState()
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var showAddEventSheet by remember { mutableStateOf(false) }
    val onDayClick = remember(viewModel) { viewModel::selectDay }

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    TextButton(onClick = { viewModel.selectDay(LocalDate.now()) }) {
                        Text("Today")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEventSheet = true },
                containerColor = crystalFabContainerColor(),
                contentColor = crystalFabContentColor(),
                elevation = crystalFabElevation()
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MonthHeader(
                    month = currentMonth,
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() }
                )
                DayOfWeekHeader()
                MonthGrid(
                    month = currentMonth,
                    selectedDayProvider = { selectedDayState.value },
                    monthIndicatorDates = monthIndicatorDates,
                    onDayClick = onDayClick,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            HorizontalDivider()

            val currentSelectedDay = selectedDay
            if (currentSelectedDay != null) {
                DayDetail(
                    selectedDay = currentSelectedDay,
                    events = selectedDayEvents,
                    onEditEvent = { editingEvent = it },
                    onDeleteEvent = { viewModel.deleteCalendarEvent(it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showAddEventSheet) {
        DesktopAddCalendarEventSheet(
            initialDate = selectedDay ?: LocalDate.now(),
            onDismiss = { showAddEventSheet = false },
            onSave = { event ->
                viewModel.addCalendarEvent(event)
                showAddEventSheet = false
            }
        )
    }

    editingEvent?.let { event ->
        DesktopAddCalendarEventSheet(
            existing = event,
            onDismiss = { editingEvent = null },
            onSave = { updated ->
                viewModel.updateCalendarEvent(updated)
                editingEvent = null
            }
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        TextButton(onClick = onPrevious) {
            Text("‹", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onNext) {
            Text("›", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun DayOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDayProvider: () -> LocalDate?,
    monthIndicatorDates: Set<String>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDayOfWeek = remember(month) { month.atDay(1).dayOfWeek.value - 1 }
    val daysInMonth = remember(month) { month.lengthOfMonth() }
    val today = remember { LocalDate.now() }
    val dayData = remember(month) {
        (1..daysInMonth).map { d -> month.atDay(d) to month.atDay(d).format(ISO_LOCAL_DATE) }
    }
    val totalCells = firstDayOfWeek + daysInMonth
    val rowCount = (totalCells + 6) / 7

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        repeat(rowCount) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                repeat(7) { colIndex ->
                    val cellIndex = rowIndex * 7 + colIndex
                    val dayIndex = cellIndex - firstDayOfWeek
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (dayIndex in 0 until daysInMonth) {
                            val (day, dateStr) = dayData[dayIndex]
                            DayCell(
                                date = day,
                                isToday = day == today,
                                hasIndicator = dateStr in monthIndicatorDates,
                                selectedDayProvider = selectedDayProvider,
                                onClick = { onDayClick(day) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    hasIndicator: Boolean,
    selectedDayProvider: () -> LocalDate?,
    onClick: () -> Unit
) {
    val isSelected = selectedDayProvider() == date
    val primary = MaterialTheme.colorScheme.primary
    val bubbleBg = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(CircleShape)
            .background(bubbleBg)
            .clickable(onClick = onClick)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (hasIndicator) {
                Canvas(modifier = Modifier.size(4.dp)) { drawCircle(color = primary) }
            }
        }
    }
}

@Composable
private fun DayDetail(
    selectedDay: LocalDate,
    events: List<CalendarEvent>,
    onEditEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMMM d") }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = selectedDay.format(formatter),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        if (events.isEmpty()) {
            Text(
                "Nothing here yet — tap + to add an event",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    CalendarEventRow(event = event, onEdit = { onEditEvent(event) }, onDelete = { onDeleteEvent(event) })
                }
            }
        }
    }
}

@Composable
private fun CalendarEventRow(event: CalendarEvent, onEdit: () -> Unit, onDelete: () -> Unit) {
    val localeFormatting = koinInject<LocaleFormatting>()
    val is24Hour = remember(localeFormatting) { localeFormatting.is24HourFormat() }
    val accentColor = resolveDisplayColor(event.colorIndex, event.colorArgb)

    EventCard(modifier = Modifier.clickable(onClick = onEdit)) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(event.title) },
            supportingContent = {
                Column {
                    if (event.note.isNotBlank()) Text(event.note)
                    val startStr = event.startTime?.let { formatTimeLabel(it, is24Hour) }
                    val endStr = event.endTime?.let { formatTimeLabel(it, is24Hour) }
                    val timeLabel = when {
                        startStr == null -> "All day"
                        endStr != null -> "$startStr – $endStr"
                        else -> startStr
                    }
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (event.startDate != event.endDate) {
                        Text(
                            text = "Until ${event.endDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            leadingContent = {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(accentColor))
            },
            trailingContent = {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
            }
        )
    }
}

@Composable
private fun EventCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.crystalCardSurface()) { content() }
}

/** Local stand-in for androidMain's internal String.toFormattedTime/formatTime (not visible cross-source-set). */
internal fun formatTimeLabel(hhmm: String, is24Hour: Boolean): String {
    val parts = hhmm.split(":")
    if (parts.size < 2) return hhmm
    val h = parts[0].toIntOrNull() ?: return hhmm
    val m = parts[1].toIntOrNull() ?: return hhmm
    return if (is24Hour) "%02d:%02d".format(h, m)
    else {
        val amPm = if (h < 12) "AM" else "PM"
        val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        "%d:%02d %s".format(h12, m, amPm)
    }
}
