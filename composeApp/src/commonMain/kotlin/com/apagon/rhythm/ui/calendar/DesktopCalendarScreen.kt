package com.apagon.rhythm.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalControlColor
import com.apagon.rhythm.ui.reminders.DesktopAddReminderSheet
import com.apagon.rhythm.ui.reminders.ReminderViewModel
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import com.apagon.rhythm.ui.util.RhythmAddFab
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stage 8's desktop Calendar tab, Stage 17e: no longer its own sidebar screen — this is
 * DesktopTodayScreen's calendar-mode content now (the real Android app has no separate Calendar
 * tab; Today's calendarMode toggle shows this instead of the habit/to-do list). Dropped its own
 * Scaffold/TopAppBar — DesktopTodayScreen owns the shared one — and the FAB is now a self-contained
 * Box overlay (same pattern DesktopJournalScreen.kt already uses without a Scaffold) instead of
 * Scaffold's floatingActionButton slot. Day-detail now also shows Reminders for the selected day
 * (reusing ReminderViewModel, previously Clock-only — matches mobile's DayDetailView.kt, which
 * shows events and reminders together, not Reminders under Alarms/Timers).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesktopTodayCalendarContent(
    viewModel: DesktopCalendarViewModel = koinViewModel(),
    reminderViewModel: ReminderViewModel = koinViewModel()
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedDayState = viewModel.selectedDay.collectAsState()
    val selectedDay by selectedDayState
    val monthIndicatorDates by viewModel.monthIndicatorDates.collectAsState()
    val selectedDayEvents by viewModel.selectedDayEvents.collectAsState()
    val upcomingReminders by reminderViewModel.upcomingReminders.collectAsState()
    val pastReminders by reminderViewModel.pastReminders.collectAsState()
    val completedReminders by reminderViewModel.completedReminders.collectAsState()
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var showAddEventSheet by remember { mutableStateOf(false) }
    var showAddReminder by remember { mutableStateOf(false) }
    val onDayClick = remember(viewModel) { viewModel::selectDay }

    Box(modifier = Modifier.fillMaxSize()) {
        // Stage 15b: horizontal split, not the vertical stack this had before — month grid on
        // the left, day-detail agenda on the right, following the TickTick "Split View" precedent
        // research turned up (list/calendar side by side, not stacked) rather than wasting the
        // window's actual width the way the vertical stack did.
        Row(modifier = Modifier.fillMaxSize()) {
            // Stage 16b: this pane previously used its own 4-8dp outer-margin scale (MonthHeader
            // and MonthGrid each padding their own horizontal edge slightly) instead of the 16dp
            // screenPadding convention every other screen uses — normalized to one shared margin
            // on the pane itself, with MonthHeader/MonthGrid's own small paddings removed below.
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(DesktopLayout.screenPadding)) {
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

            VerticalDivider()

            val currentSelectedDay = selectedDay
            if (currentSelectedDay != null) {
                val dayReminders = remember(currentSelectedDay, upcomingReminders, pastReminders, completedReminders) {
                    val dateStr = currentSelectedDay.format(ISO_LOCAL_DATE)
                    (upcomingReminders + pastReminders + completedReminders).filter { it.dateTime.substringBefore(" ") == dateStr }
                }
                DayDetail(
                    selectedDay = currentSelectedDay,
                    events = selectedDayEvents,
                    onEditEvent = { editingEvent = it },
                    onDeleteEvent = { viewModel.deleteCalendarEvent(it) },
                    reminders = dayReminders,
                    onToggleReminder = { reminderViewModel.toggleCompletion(it) },
                    onDeleteReminder = { reminderViewModel.deleteReminder(it) },
                    onAddReminder = { showAddReminder = true },
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(DesktopLayout.screenPadding)
                )
            }
        }

        RhythmAddFab(
            onClick = { showAddEventSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(DesktopLayout.screenPadding)
        )
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

    if (showAddReminder) {
        DesktopAddReminderSheet(
            onDismiss = { showAddReminder = false },
            onSave = { title, note, dateTime ->
                reminderViewModel.addReminder(title, note, dateTime)
                showAddReminder = false
            }
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(onClick = onPrevious) {
            Text("‹", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
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

    Column(modifier = modifier.fillMaxWidth()) {
        repeat(rowCount) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                repeat(7) { colIndex ->
                    val cellIndex = rowIndex * 7 + colIndex
                    val dayIndex = cellIndex - firstDayOfWeek
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
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
        isSelected -> crystalControlColor(MaterialTheme.colorScheme.primaryContainer)
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }
    // Stage 17c: the cell's actual grid slot isn't square (7 columns vs. a variable row count
    // splitting whatever height MonthGrid has left), so CircleShape on a non-square Box drew an
    // oval/pill instead of a circle. Forcing 1:1 before the clip makes it a true circle regardless
    // of the slot's real dimensions — the parent Box (MonthGrid) now centers it in that slot.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .aspectRatio(1f)
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
    reminders: List<Reminder>,
    onToggleReminder: (Reminder) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    onAddReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMMM d") }

    // Stage 16b: horizontal margin already comes from the pane's own DesktopLayout.screenPadding
    // (added at the call site in DesktopCalendarScreen), so these no longer add their own.
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = selectedDay.format(formatter),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        if (events.isEmpty() && reminders.isEmpty()) {
            Text(
                "Nothing here yet — tap + to add an event",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(events, key = { "event_${it.id}" }) { event ->
                    CalendarEventRow(event = event, onEdit = { onEditEvent(event) }, onDelete = { onDeleteEvent(event) })
                }
                items(reminders, key = { "reminder_${it.id}" }) { reminder ->
                    DesktopReminderRow(reminder, onToggle = { onToggleReminder(reminder) }, onDelete = { onDeleteReminder(reminder) })
                }
            }
        }
        TextButton(onClick = onAddReminder, modifier = Modifier.padding(top = 4.dp)) { Text("+ Reminder") }
    }
}

// Stage 17e: moved from DesktopClockScreen.kt (Reminders is no longer Clock's — it lives on
// Today's Calendar day-detail, matching mobile's DayDetailView.kt).
@Composable
internal fun DesktopReminderRow(reminder: Reminder, onToggle: () -> Unit, onDelete: () -> Unit) {
    Box(Modifier.fillMaxWidth().crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesktopLayout.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(reminder.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(reminder.dateTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (reminder.note.isNotBlank()) {
                    Text(reminder.note, style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = onToggle) { Text(if (reminder.isCompleted) "Undo" else "Complete") }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Composable
internal fun CalendarEventRow(event: CalendarEvent, onEdit: () -> Unit, onDelete: () -> Unit) {
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
