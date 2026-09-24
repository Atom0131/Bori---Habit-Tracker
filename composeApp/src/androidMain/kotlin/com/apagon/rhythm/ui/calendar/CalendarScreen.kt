package com.apagon.rhythm.ui.calendar
import com.apagon.rhythm.core.time.*

import org.koin.compose.viewmodel.koinViewModel

import com.apagon.rhythm.ui.util.HabitCard
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import android.text.format.DateFormat
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.ui.reminders.AddReminderSheet
import com.apagon.rhythm.ui.util.findActivity
import com.apagon.rhythm.ui.util.formatAsReminderTime
import com.apagon.rhythm.ui.util.ProPaywallSheet
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import com.apagon.rhythm.ui.util.toFormattedTime
import com.apagon.rhythm.ui.util.resolvedIcon
import com.apagon.rhythm.ui.util.ordinal
import com.apagon.rhythm.ui.util.scheduleLabel
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.YearMonth
import com.apagon.rhythm.core.time.DateTimeFormatter
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE
import java.util.Locale

import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = koinViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedDayState = viewModel.selectedDay.collectAsState()
    val selectedDay by selectedDayState
    val currentSelectedDay = selectedDay
    val monthIndicatorDates by viewModel.monthIndicatorDates.collectAsState()
    val selectedDayReminders by viewModel.selectedDayReminders.collectAsState()
    val selectedDayEvents by viewModel.selectedDayEvents.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val monthCompletions by viewModel.monthCompletions.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val availableCalendars by viewModel.availableCalendars.collectAsState()
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var showAddEventSheet by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var paywallReason by remember { mutableStateOf<String?>(null) }
    val onDayClick = remember(viewModel) { viewModel::selectDay }
    val context = LocalContext.current

    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { 
        screenVisible = true 
        viewModel.refreshAvailableCalendars()
    }

    AnimatedVisibility(
        visible = screenVisible,
        enter = fadeIn(tween(350, easing = EaseInOut)),
    ) {
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    IconButton(onClick = {
                        viewModel.selectDay(LocalDate.now())
                        // Assuming selectDay also jumps to the month, but if not we might need a jumpToToday in VM
                    }) {
                        Icon(Icons.Default.Today, contentDescription = "Go to Today")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            ) 
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddEventSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add event")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top half: full-width calendar
            Column(modifier = Modifier.weight(1f)) {
                MonthHeader(
                    month = currentMonth,
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() }
                )
                DayOfWeekHeader()
                AnimatedContent(
                    targetState = currentMonth,
                    transitionSpec = { fadeIn(tween(300, easing = EaseInOut)) togetherWith fadeOut(tween(300, easing = EaseInOut)) },
                    modifier = Modifier.weight(1f),
                    label = "MonthGridTransition"
                ) { month ->
                    MonthGrid(
                        month = month,
                        selectedDayProvider = { selectedDayState.value },
                        monthIndicatorDates = monthIndicatorDates,
                        onDayClick = onDayClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            HorizontalDivider()

            // Bottom half: events for selected day
            if (currentSelectedDay != null) {
                DayDetail(
                    selectedDay = currentSelectedDay,
                    reminders = selectedDayReminders,
                    events = selectedDayEvents,
                    onToggleReminderComplete = { viewModel.toggleReminderCompletion(it) },
                    onEditReminder = { editingReminder = it },
                    onEditEvent = { editingEvent = it },
                    onDeleteEvent = { viewModel.deleteCalendarEvent(it) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                val completionsByHabit = remember(monthCompletions) {
                    buildMap<Long, MutableSet<Int>> {
                        monthCompletions.forEach { (dateStr, ids) ->
                            val day = LocalDate.parse(dateStr, ISO_LOCAL_DATE).dayOfMonth
                            ids.forEach { id -> getOrPut(id) { mutableSetOf() }.add(day) }
                        }
                    }
                }
                HabitsMonthOverview(
                    habits = habits,
                    month = currentMonth,
                    completionsByHabit = completionsByHabit,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    } // AnimatedVisibility

    editingReminder?.let { reminder ->
        AddReminderSheet(
            existing = reminder,
            onDismiss = { editingReminder = null },
            onSave = { title, note, dateTime, soundUri ->
                viewModel.updateReminder(reminder.copy(title = title, note = note, dateTime = dateTime, soundUri = soundUri))
                editingReminder = null
            }
        )
    }

    if (showAddEventSheet) {
        AddCalendarEventSheet(
            initialDate = selectedDay ?: LocalDate.now(),
            isPro = isPro,
            availableCalendars = availableCalendars,
            onDismiss = { showAddEventSheet = false },
            onShowPaywall = {
                paywallReason = it
                showPaywall = true
            },
            onSave = { event, targetId ->
                viewModel.addCalendarEvent(event, targetId)
                showAddEventSheet = false
            }
        )
    }

    editingEvent?.let { event ->
        AddCalendarEventSheet(
            existing = event,
            isPro = isPro,
            onDismiss = { editingEvent = null },
            onShowPaywall = {
                paywallReason = it
                showPaywall = true
            },
            onSave = { updated, _ ->
                viewModel.updateCalendarEvent(updated)
                editingEvent = null
            }
        )
    }

    if (showPaywall) {
        ProPaywallSheet(
            reason = paywallReason,
            onDismiss = { 
                showPaywall = false
                paywallReason = null
            },
            onUpgrade = { productId ->
                viewModel.startBillingFlow(productId)
                showPaywall = false
                paywallReason = null
            }
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
        }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
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
    val firstDayOfWeek = remember(month) { month.atDay(1).dayOfWeek.value - 1 } // 0 = Monday
    val daysInMonth = remember(month) { month.lengthOfMonth() }
    val today = remember { LocalDate.now() }
    // Precompute (LocalDate, dateString) pairs once per month — avoids atDay() + format() per item per recomposition
    val dayData = remember(month) {
        (1..daysInMonth).map { d -> month.atDay(d) to month.atDay(d).format(ISO_LOCAL_DATE) }
    }

    val totalCells = firstDayOfWeek + daysInMonth
    val rowCount = (totalCells + 6) / 7

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        repeat(rowCount) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                repeat(7) { colIndex ->
                    val cellIndex = rowIndex * 7 + colIndex
                    val dayIndex = cellIndex - firstDayOfWeek
                    key(cellIndex) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            if (dayIndex in 0 until daysInMonth) {
                                val (day, dateStr) = dayData[dayIndex]
                                val onClick = remember(day) { { onDayClick(day) } }
                                DayCell(
                                    date = day,
                                    isToday = day == today,
                                    hasIndicator = dateStr in monthIndicatorDates,
                                    selectedDayProvider = selectedDayProvider,
                                    onClick = onClick
                                )
                            }
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
    val day = date.dayOfMonth
    val primary = MaterialTheme.colorScheme.primary
    val bubbleBg = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday    -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else       -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
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
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    isToday    -> MaterialTheme.colorScheme.primary
                    else       -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (hasIndicator) {
                Canvas(modifier = Modifier.size(4.dp)) {
                    drawCircle(color = primary)
                }
            }
        }
    }
}

@Composable
private fun DayDetail(
    selectedDay: LocalDate,
    reminders: List<Reminder>,
    events: List<CalendarEvent>,
    onToggleReminderComplete: (Reminder) -> Unit,
    onEditReminder: (Reminder) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMMM") }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "${selectedDay.format(formatter)} ${selectedDay.dayOfMonth.ordinal()}",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        if (reminders.isEmpty() && events.isEmpty()) {
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
                // Reminders section
                if (reminders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Reminders",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderCalendarRow(
                            reminder = reminder,
                            onToggleComplete = { onToggleReminderComplete(reminder) },
                            onEdit = { onEditReminder(reminder) }
                        )
                    }
                }

                // Events section
                if (events.isNotEmpty()) {
                    item {
                        Text(
                            text = "Events",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(events, key = { "event_${it.id}" }) { event ->
                        CalendarEventRow(
                            event = event,
                            onEdit = { onEditEvent(event) },
                            onDelete = { onDeleteEvent(event) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderCalendarRow(
    reminder: Reminder,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val timeStr = remember(reminder.dateTime, is24Hour) { reminder.dateTime.formatAsReminderTime(is24Hour) }
    HabitCard(isDone = reminder.isCompleted) {
        ListItem(
            modifier = Modifier.clickable(onClick = onEdit),
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            headlineContent = {
                Text(
                    reminder.title,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
            },
            supportingContent = {
                Column {
                    if (reminder.note.isNotBlank()) Text(reminder.note)
                    if (timeStr.isNotBlank()) {
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Reminder",
                    tint = if (reminder.isCompleted) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                IconButton(onClick = onToggleComplete) {
                    Icon(
                        imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (reminder.isCompleted) "Mark incomplete" else "Mark complete",
                        tint = if (reminder.isCompleted) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun HabitsMonthOverview(
    habits: List<Habit>,
    month: YearMonth,
    completionsByHabit: Map<Long, Set<Int>>,
    modifier: Modifier = Modifier
) {
    if (habits.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "No habits yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(habits, key = { it.id }) { habit ->
            HabitMonthCard(
                habit = habit,
                month = month,
                completedDays = completionsByHabit[habit.id] ?: emptySet()
            )
        }
    }
}

@Composable
private fun HabitMonthCard(
    habit: Habit,
    month: YearMonth,
    completedDays: Set<Int>
) {
    val habitColor = resolveDisplayColor(habit.colorIndex, habit.colorArgb)
    val habitIcon = habit.resolvedIcon()

    HabitCard {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: icon + name + frequency
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(habitColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = habitIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        habit.scheduleLabel(short = true),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mini calendar
            val firstDayOfWeek = remember(month) { month.atDay(1).dayOfWeek.value - 1 } // Mon=0
            val daysInMonth = remember(month) { month.lengthOfMonth() }
            val rowCount = remember(firstDayOfWeek, daysInMonth) { (firstDayOfWeek + daysInMonth + 6) / 7 }
            val monthLabel = remember(month) {
                month.format(DateTimeFormatter.ofPattern("MMMM"))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(habitColor.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Day-of-week headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { d ->
                        Text(
                            text = d,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Calendar grid
                repeat(rowCount) { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(7) { col ->
                            val dayNum = row * 7 + col - firstDayOfWeek + 1
                            Box(
                                modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNum in 1..daysInMonth) {
                                    val isCompleted = dayNum in completedDays
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(if (isCompleted) habitColor else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCompleted) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Streak count
            Text(
                text = "${completedDays.size} streaks this month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CalendarEventRow(
    event: CalendarEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    val accentColor = resolveDisplayColor(event.colorIndex, event.colorArgb)
    val isExternal = event.id < 0

    HabitCard(
        modifier = Modifier.clickable(onClick = onEdit)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.title)
                    if (isExternal) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "External event",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            supportingContent = {
                Column {
                    if (event.note.isNotBlank()) Text(event.note)
                    
                    val startStr = event.startTime?.toFormattedTime(is24Hour)
                    val endStr = event.endTime?.toFormattedTime(is24Hour)
                    
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
                    if (isExternal && event.calendarDisplayName != null) {
                        Text(
                            text = "${event.calendarDisplayName} (${event.accountName})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Event",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        )
    }
}
