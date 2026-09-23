package com.apagon.rhythm.ui.habit
import com.apagon.rhythm.core.time.*

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistProgress
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.ui.stats.HabitYearStats
import com.apagon.rhythm.ui.util.*
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayDetailView(
    innerPadding: PaddingValues,
    date: LocalDate,
    groupedHabits: Map<HabitFrequency, List<Habit>>,
    todayCompletions: Set<Long>,
    todayChecklistProgress: Map<Long, ChecklistProgress>,
    overdueTodos: List<Todo>,
    dueTodayTodos: List<Todo>,
    completedTodos: List<Todo>,
    selectedDayEvents: List<CalendarEvent>,
    overdueReminders: List<Reminder>,
    completedRemindersToday: List<Reminder>,
    selectedDayReminders: List<Reminder>,
    selectedDateTodos: List<Todo>,
    journalEntries: List<JournalEntry>,
    habitStats: List<HabitYearStats>,
    onBack: () -> Unit,
    onToggleCompletion: (Long, Boolean) -> Unit,
    onToggleItem: (Long, ChecklistItem, Boolean) -> Unit,
    onArchive: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onEdit: (Habit) -> Unit,
    onView: (Habit) -> Unit,
    onTodoToggle: (Todo) -> Unit,
    onTodoEdit: (Todo?) -> Unit,
    onTodoDelete: (Todo) -> Unit,
    onEventEdit: (CalendarEvent) -> Unit,
    onEventDelete: (CalendarEvent) -> Unit,
    onReminderEdit: (Reminder) -> Unit,
    onReminderToggle: (Reminder) -> Unit,
    onReminderDelete: (Reminder) -> Unit,
    onStatClick: (HabitYearStats) -> Unit
) {
    val dateLabel = remember(date) {
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    }
    val allHabits = remember(groupedHabits) { groupedHabits.values.flatten() }
    val dayTotal = remember(allHabits) { allHabits.size }
    val dayDone = remember(allHabits, todayCompletions) { allHabits.count { it.id in todayCompletions } }
    val completedHabits = remember(allHabits, todayCompletions) { allHabits.filter { it.id in todayCompletions } }
    var completedExpanded by remember { mutableStateOf(false) }
    var overdueTodosExpanded by remember { mutableStateOf(false) }
    var completedTodosExpanded by remember { mutableStateOf(false) }
    var journalExpanded by remember { mutableStateOf(true) }

    val pendingGroupedHabits = remember(groupedHabits, todayCompletions) {
        groupedHabits.mapValues { (_, habits) -> habits.filter { it.id !in todayCompletions } }
    }

    LazyColumn(
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 100.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top bar
        item(key = "detail_topbar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(48.dp))
            }
        }

        // Completion pill + progress bar
        if (dayTotal > 0) {
            item(key = "detail_progress") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "$dayDone/$dayTotal Habits Done",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { dayDone.toFloat() / dayTotal.coerceAtLeast(1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Incomplete habits grouped by frequency
        pendingGroupedHabits.forEach { (frequency, habitsInGroup) ->
            if (habitsInGroup.isNotEmpty()) {
                stickyHeader(key = "detail_${frequency.name}_header") {
                    SectionHeader(frequency = frequency)
                }
                items(habitsInGroup, key = { "detail_${it.id}" }) { habit ->
                    HabitRow(
                        habit = habit,
                        isDone = false,
                        checklistProgress = todayChecklistProgress[habit.id],
                        onToggle = { onToggleCompletion(habit.id, false) },
                        onToggleItem = { item, itemIsDone -> onToggleItem(habit.id, item, itemIsDone) },
                        onArchive = { onArchive(habit) },
                        onDelete = { onDeleteHabit(habit) },
                        onEdit = { onEdit(habit) },
                        onView = { onView(habit) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // Collapsible completed section
        if (completedHabits.isNotEmpty()) {
            stickyHeader(key = "detail_completed_header") {
                CollapsibleSectionHeader(
                    title = "Completed (${completedHabits.size})",
                    expanded = completedExpanded,
                    onToggle = { completedExpanded = !completedExpanded }
                )
            }
            if (completedExpanded) {
                items(completedHabits, key = { "detail_done_${it.id}" }) { habit ->
                    HabitRow(
                        habit = habit,
                        isDone = true,
                        checklistProgress = todayChecklistProgress[habit.id],
                        onToggle = { onToggleCompletion(habit.id, true) },
                        onToggleItem = { item, itemIsDone -> onToggleItem(habit.id, item, itemIsDone) },
                        onArchive = { onArchive(habit) },
                        onDelete = { onDeleteHabit(habit) },
                        onEdit = { onEdit(habit) },
                        onView = { onView(habit) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        eventsSection(
            events = selectedDayEvents,
            onEventEdit = onEventEdit,
            onEventDelete = onEventDelete
        )

        // Journal entries for this day
        if (journalEntries.isNotEmpty()) {
            stickyHeader(key = "detail_journal_header") {
                CollapsibleSectionHeader(
                    title = "Journal (${journalEntries.size})",
                    expanded = journalExpanded,
                    onToggle = { journalExpanded = !journalExpanded }
                )
            }
            if (journalExpanded) {
                items(journalEntries, key = { "journal_${it.id}" }) { entry ->
                    HabitCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (entry.title.isNotEmpty()) {
                                Text(
                                    entry.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (entry.content.isNotEmpty()) {
                                Text(
                                    entry.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (entry.mood > 0) {
                                val moodEmojis = listOf("", "😞", "😕", "😐", "🙂", "😊")
                                Text(
                                    moodEmojis.getOrElse(entry.mood) { "" },
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        todosSection(
            overdueTodos = overdueTodos,
            dueTodayTodos = selectedDateTodos,
            dueTodayReminders = selectedDayReminders,
            overdueReminders = overdueReminders,
            overdueTodosExpanded = overdueTodosExpanded,
            onToggleOverdueExpanded = { overdueTodosExpanded = !overdueTodosExpanded },
            onTodoToggle = onTodoToggle,
            onTodoEdit = onTodoEdit,
            onTodoDelete = onTodoDelete,
            onReminderToggle = onReminderToggle,
            onReminderEdit = onReminderEdit,
            onReminderDelete = onReminderDelete
        )

        statsSection(
            habitStats = habitStats,
            onStatClick = onStatClick
        )
    }
}

@Composable
fun SectionHeader(frequency: HabitFrequency, prefix: String? = null) {
    val label = when (frequency) {
        HabitFrequency.DAILY -> "Daily Habits"
        HabitFrequency.WEEKLY -> "Weekly Habits"
        HabitFrequency.MONTHLY -> "Monthly Habits"
    }
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (prefix != null) "$prefix · $label" else label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.eventsSection(
    events: List<CalendarEvent>,
    onEventEdit: (CalendarEvent) -> Unit,
    onEventDelete: (CalendarEvent) -> Unit
) {
    if (events.isNotEmpty()) {
        stickyHeader(key = "events_header") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = "Events",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }
        items(events, key = { "event_${it.id}" }) { event ->
            CalendarEventRow(
                event = event,
                onEdit = { onEventEdit(event) },
                onDelete = { onEventDelete(event) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEventRow(
    event: CalendarEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    val accentColor = resolveDisplayColor(event.colorIndex, event.colorArgb)
    val isExternal = event.id < 0

    com.apagon.rhythm.ui.util.SwipeToDeleteBox(
        onDelete = onDelete,
        deleteContentDescription = "Delete Event",
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        HabitCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit() },
            isDone = false
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
                        val timeLabel = when {
                            event.startTime == null -> "All day"
                            event.endTime != null -> {
                                val s = event.startTime.toFormattedTime(is24Hour)
                                val e = event.endTime.toFormattedTime(is24Hour)
                                "$s – $e"
                            }
                            else -> event.startTime.toFormattedTime(is24Hour)
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
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.remindersSection(
    reminders: List<Reminder>,
    onReminderEdit: (Reminder) -> Unit,
    onReminderToggle: (Reminder) -> Unit,
    onReminderDelete: (Reminder) -> Unit
) {
    if (reminders.isNotEmpty()) {
        stickyHeader(key = "reminders_header") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = "Reminders",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }
        items(reminders, key = { "reminder_${it.id}" }) { reminder ->
            ReminderRow(
                reminder = reminder,
                onToggle = { onReminderToggle(reminder) },
                onEdit = { onReminderEdit(reminder) },
                onDelete = { onReminderDelete(reminder) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderRow(
    reminder: Reminder,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val is24Hour = remember { context.isSystem24Hour() }
    val containerColor = if (reminder.isCompleted)
        MaterialTheme.colorScheme.surfaceContainerLow
    else
        MaterialTheme.colorScheme.surfaceContainerHigh

    com.apagon.rhythm.ui.util.SwipeToDeleteBox(
        onDelete = onDelete,
        deleteContentDescription = "Delete Reminder",
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        HabitCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit() },
            isDone = reminder.isCompleted
        ) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    Text(
                        text = reminder.title,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color.Unspecified
                    )
                },
                supportingContent = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = reminder.dateTime.formatAsReminderDateTime(is24Hour),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (reminder.note.isNotBlank()) {
                            Text(
                                text = reminder.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                trailingContent = {
                    IconButton(onClick = onToggle) {
                        Icon(
                            imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (reminder.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.todosSection(
    overdueTodos: List<Todo>,
    dueTodayTodos: List<Todo>,
    dueTodayReminders: List<Reminder>,
    overdueReminders: List<Reminder>,
    overdueTodosExpanded: Boolean,
    onToggleOverdueExpanded: () -> Unit,
    onTodoToggle: (Todo) -> Unit,
    onTodoEdit: (Todo?) -> Unit,
    onTodoDelete: (Todo) -> Unit,
    onReminderToggle: (Reminder) -> Unit,
    onReminderEdit: (Reminder) -> Unit,
    onReminderDelete: (Reminder) -> Unit
) {
    stickyHeader(key = "todos_header") {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "To-do List",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }

    // Active todos and reminders (due today + no-date)
    if (dueTodayTodos.isEmpty() && dueTodayReminders.isEmpty() && overdueTodos.isEmpty() && overdueReminders.isEmpty()) {
        item(key = "todos_empty") {
            Text(
                "No to-do's — tap + to add one",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    } else {
        items(dueTodayTodos, key = { "todo_${it.id}" }) { todo ->
            com.apagon.rhythm.ui.todos.TodoRow(
                todo = todo,
                onToggle = { onTodoToggle(todo) },
                onEdit = { onTodoEdit(todo) },
                onDelete = { onTodoDelete(todo) },
                modifier = Modifier.animateItem()
            )
        }
        items(dueTodayReminders, key = { "reminder_active_${it.id}" }) { reminder ->
            ReminderRow(
                reminder = reminder,
                onToggle = { onReminderToggle(reminder) },
                onEdit = { onReminderEdit(reminder) },
                onDelete = { onReminderDelete(reminder) },
                modifier = Modifier.animateItem()
            )
        }
    }

    // Overdue section — below active todos, above completed
    if (overdueTodos.isNotEmpty() || overdueReminders.isNotEmpty()) {
        stickyHeader(key = "todos_overdue_header") {
            CollapsibleSectionHeader(
                title = "Overdue (${overdueTodos.size + overdueReminders.size})",
                expanded = overdueTodosExpanded,
                onToggle = onToggleOverdueExpanded
            )
        }
        if (overdueTodosExpanded) {
            items(overdueTodos, key = { "todo_overdue_${it.id}" }) { todo ->
                com.apagon.rhythm.ui.todos.TodoRow(
                    todo = todo,
                    onToggle = { onTodoToggle(todo) },
                    onEdit = { onTodoEdit(todo) },
                    onDelete = { onTodoDelete(todo) },
                    modifier = Modifier.animateItem()
                )
            }
            items(overdueReminders, key = { "reminder_overdue_${it.id}" }) { reminder ->
                ReminderRow(
                    reminder = reminder,
                    onToggle = { onReminderToggle(reminder) },
                    onEdit = { onReminderEdit(reminder) },
                    onDelete = { onReminderDelete(reminder) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}
