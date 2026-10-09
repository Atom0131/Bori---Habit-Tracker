package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.platform.ImageBitmapLoader
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE
import com.apagon.rhythm.ui.calendar.DesktopCalendarViewModel
import com.apagon.rhythm.ui.calendar.eventsSection
import com.apagon.rhythm.ui.reminders.ReminderViewModel
import com.apagon.rhythm.ui.reminders.remindersForDate
import com.apagon.rhythm.ui.reminders.remindersSection
import com.apagon.rhythm.ui.todos.TodoViewModel
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.todos.todoSection
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.key
import com.apagon.rhythm.core.time.now
import kotlinx.datetime.LocalDateTime
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import com.apagon.rhythm.ui.todos.DesktopAddTodoSheet
import com.apagon.rhythm.data.model.Todo

// Stage 3's minimal desktop Habit screen, extended for the layout-parity round to match the
// phone app's organization: a greeting/streak/%-done header and Daily/Weekly/Monthly collapsible
// sections (ui/habit/HabitListScreen.kt, HabitFrequencySection.kt on Android), while deliberately
// staying a plain list rather than a full port (no checklist items, no swipe actions, no per-habit
// edit sheet — none of those exist on desktop today). Grouped/streak/%-done state comes from
// HabitListViewModel, which already implements it and just wasn't wired into Koin until now;
// DesktopHabitViewModel is kept for its desktop-only sync/QR/add-habit responsibilities.
// Stage 17e: the real Android app has no separate Habits/To-dos/Calendar tabs — HabitListScreen.kt
// (the "Today" tab) shows habits and to-dos together in one continuous list. This is now
// DesktopTodayScreen's list-mode content: habit sections plus, via DesktopHabitList's extraItems
// hook, a to-do section appended in the same LazyColumn. No longer called directly from the
// sidebar (DesktopTodayScreen.kt owns that now) — kept as its own composable since the
// selectedHabitId/detail-pane split-pane logic (Stage 15e) is still exactly right for list mode.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesktopTodayListContent(
    viewModel: DesktopHabitViewModel = koinViewModel(),
    habitListViewModel: HabitListViewModel = koinViewModel(),
    todoViewModel: TodoViewModel = koinViewModel(),
    calendarViewModel: DesktopCalendarViewModel = koinViewModel(),
    reminderViewModel: ReminderViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    // Stage 19f: Android's Today list always shows a WeekStrip between the header and the habit
    // sections — desktop had none. selectedDate drives both the strip and (Stage 19g) the
    // Reminders/Events sections below the habit sections, kept in sync with calendarViewModel's
    // own selectedDay so Today list-mode and Today calendar-mode agree on "the selected day".
    val calendarSelectedDay by calendarViewModel.selectedDay.collectAsState()
    var selectedDate by remember { mutableStateOf(calendarSelectedDay ?: LocalDate.now()) }
    var weekStripExpanded by remember { mutableStateOf(false) }
    // The week strip dots only days with an event or reminder, exactly like Android's Today
    // (`HabitListScreen` passes `calendarEventDates`). It used the calendar's month indicators,
    // which also mark every day a habit is due, so a daily habit put a dot under every day.
    val weekStripDates by habitListViewModel.calendarEventDates.collectAsState()

    // Stage 19g: Reminders/Events, previously only reachable via Calendar mode's day-detail —
    // Android's Today list always shows them inline, after the habit sections and before To-dos.
    val upcomingReminders by reminderViewModel.upcomingReminders.collectAsState()
    val pastReminders by reminderViewModel.pastReminders.collectAsState()
    val completedReminders by reminderViewModel.completedReminders.collectAsState()
    val selectedDayReminders = remember(selectedDate, upcomingReminders, pastReminders, completedReminders) {
        remindersForDate(upcomingReminders + pastReminders + completedReminders, selectedDate.format(ISO_LOCAL_DATE))
    }
    val selectedDayEvents by calendarViewModel.selectedDayEvents.collectAsState()
    var remindersExpanded by remember { mutableStateOf(false) }
    var eventsExpanded by remember { mutableStateOf(false) }
    // Stage 9: tapping a habit navigates to DesktopHabitDetailScreen (Stats).
    // Plain local state, matching this project's existing showX/editingX
    // toggle pattern rather than a real navigation library (Stage 11 territory).
    var selectedHabitId by remember { mutableStateOf<Long?>(null) }

    val themePreferences = koinInject<ThemePreferences>()
    val userName by themePreferences.userName.collectAsState(initial = "")
    val profilePictureUri by themePreferences.profilePictureUri.collectAsState(initial = null)

    val habitsUiState by habitListViewModel.habitsUiState.collectAsState()
    val dailyStreak by habitListViewModel.dailyStreak.collectAsState()
    val completionRate by habitListViewModel.completionRate.collectAsState()

    // The selected day's to-dos, bucketed exactly as Android's Today does.
    val dueTodayTodos by habitListViewModel.selectedDateTodos.collectAsState()
    val overdueTodos by habitListViewModel.selectedDateOverdueTodos.collectAsState()
    val completedTodos by habitListViewModel.selectedDateCompletedTodos.collectAsState()
    var todosOverdueExpanded by remember { mutableStateOf(false) }
    var todosCompletedExpanded by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    val subtasksByTodo by todoViewModel.subtasksByTodo.collectAsState()
    // Stage 18: matches the phone app's HabitListScreen — its "todos" section uses the exact same
    // collapsedSections set as the habit frequency sections, seeded closed. Kept as its own flag
    // here rather than widening collapsedSections' type, since To-dos isn't a HabitFrequency.
    var todosExpanded by remember { mutableStateOf(false) }

    // Seeded fully closed, matching the phone app's HabitListScreen: the planner opens with every
    // primary section closed rather than in a mix of open/closed states.
    LaunchedEffect(selectedDate) { habitListViewModel.setSelectedDate(selectedDate) }

    var habitsExpanded by remember { mutableStateOf(false) }
    var habitsDoneExpanded by remember { mutableStateOf(false) }
    // Android's pencil / long-press: the add-habit sheet in edit mode. Desktop had no way to edit a
    // habit before this (2026-10-09).
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    // Stage 15e: the habit list stays visible as a left column when a habit is selected, with
    // DesktopHabitDetailScreen filling a right pane instead of replacing the whole screen — lighter
    // than Notes/Journal's persistent-rail treatment (Habits is this app's primary landing screen,
    // not a browse-then-drill-down surface), but the same "don't discard the list" principle.
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = if (selectedHabitId != null) Modifier.weight(1f).fillMaxHeight() else Modifier.fillMaxSize()) {
            DesktopHabitList(
                viewModel = viewModel,
                habitListViewModel = habitListViewModel,
                state = state,
                selectedDate = selectedDate,
                onSelectedDateChange = {
                    selectedDate = it
                    calendarViewModel.selectDay(it)
                },
                weekStripExpanded = weekStripExpanded,
                onToggleWeekStripExpanded = { weekStripExpanded = !weekStripExpanded },
                weekStripIndicatorDates = weekStripDates,
                userName = userName,
                profilePictureUri = profilePictureUri,
                habitsUiState = habitsUiState,
                dailyStreak = dailyStreak,
                completionRate = completionRate,
                habitsExpanded = habitsExpanded,
                onToggleHabitsExpanded = { habitsExpanded = !habitsExpanded },
                habitsDoneExpanded = habitsDoneExpanded,
                onToggleHabitsDone = { habitsDoneExpanded = !habitsDoneExpanded },
                onSelectHabit = { selectedHabitId = it },
                onEditHabit = { editingHabit = it },
                remindersEventsItems = {
                    remindersSection(
                        reminders = selectedDayReminders,
                        expanded = remindersExpanded,
                        onToggleExpanded = { remindersExpanded = !remindersExpanded },
                        onToggle = { reminderViewModel.toggleCompletion(it) },
                        onDelete = { reminderViewModel.deleteReminder(it) }
                    )
                    eventsSection(
                        events = selectedDayEvents,
                        expanded = eventsExpanded,
                        onToggleExpanded = { eventsExpanded = !eventsExpanded },
                        onEdit = { /* Stage 19g: no desktop edit-event entry point from Today list yet — Calendar mode has one. */ },
                        onDelete = { calendarViewModel.deleteCalendarEvent(it) }
                    )
                },
                extraItems = {
                    todoSection(
                        dueToday = dueTodayTodos,
                        overdue = overdueTodos,
                        completed = completedTodos,
                        expanded = todosExpanded,
                        onToggleExpanded = { todosExpanded = !todosExpanded },
                        overdueExpanded = todosOverdueExpanded,
                        onToggleOverdueExpanded = { todosOverdueExpanded = !todosOverdueExpanded },
                        completedExpanded = todosCompletedExpanded,
                        onToggleCompletedExpanded = { todosCompletedExpanded = !todosCompletedExpanded },
                        onToggle = { todoViewModel.toggleCompletion(it) },
                        onEdit = { editingTodo = it },
                        subtasksByTodo = subtasksByTodo,
                        onToggleSubtask = { todoViewModel.toggleSubtask(it) }
                    )
                }
            )
        }

        selectedHabitId?.let { habitId ->
            VerticalDivider()
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                DesktopHabitDetailScreen(habitId = habitId, onBack = { selectedHabitId = null })
            }
        }
    }

    editingTodo?.let { todo ->
        key(todo.id) {
            DesktopAddTodoSheet(
                onDismiss = { editingTodo = null },
                existing = todo,
                existingSubtasks = subtasksByTodo[todo.id].orEmpty(),
                onDelete = { todoViewModel.deleteTodo(todo); editingTodo = null },
                onSave = { title, note, dueDate, priority, iconIndex, subtasks ->
                    todoViewModel.updateTodo(todo, title, note, dueDate, priority, iconIndex, subtasks = subtasks)
                    editingTodo = null
                }
            )
        }
    }

    editingHabit?.let { habit ->
        val items by remember(habit.id) { habitListViewModel.getItemsForHabit(habit.id) }.collectAsState(initial = emptyList())
        // Keyed on the habit so opening a different one starts from its own values.
        key(habit.id) {
            DesktopAddHabitSheet(
                onDismiss = { editingHabit = null },
                initialHabit = habit,
                initialChecklist = items.sortedBy { it.sortOrder }.map { it.label },
                onSave = { name, description, frequency, weekDaysMask, monthDaysMask, isChecklist, checklistItems,
                           colorIndex, colorArgb, durationDays, iconIndex, reminderTime ->
                    habitListViewModel.updateHabit(
                        habit, name, description, frequency, weekDaysMask, monthDaysMask, isChecklist, checklistItems,
                        colorIndex, colorArgb, durationDays, iconIndex, reminderTime
                    )
                    editingHabit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesktopHabitList(
    viewModel: DesktopHabitViewModel,
    habitListViewModel: HabitListViewModel,
    state: DesktopHabitUiState,
    selectedDate: LocalDate,
    onSelectedDateChange: (LocalDate) -> Unit,
    weekStripExpanded: Boolean,
    onToggleWeekStripExpanded: () -> Unit,
    weekStripIndicatorDates: Set<String>,
    userName: String,
    profilePictureUri: String?,
    habitsUiState: HabitsUiState,
    dailyStreak: Int,
    completionRate: Int,
    habitsExpanded: Boolean,
    onToggleHabitsExpanded: () -> Unit,
    habitsDoneExpanded: Boolean,
    onToggleHabitsDone: () -> Unit,
    onSelectHabit: (Long) -> Unit,
    onEditHabit: (Habit) -> Unit,
    remindersEventsItems: LazyListScope.() -> Unit = {},
    extraItems: LazyListScope.() -> Unit = {}
) {
    // Stage 17e: no longer owns its own Scaffold — DesktopTodayScreen provides the single shared
    // Scaffold/TopAppBar/background for the whole Today screen (list mode + calendar mode both
    // live under it), so this is just the scrollable content now. extraItems lets Today append the
    // to-do section after the habit sections in the same LazyColumn, matching how the real Android
    // "Today" screen shows habits and to-dos in one continuous list, not two separate screens.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Stage 16a: same centered content-width cap Stage 15g gave To-dos/Clock, now applied
        // here too so every primary screen's card column reads the same width.
        LazyColumn(modifier = Modifier.fillMaxHeight().widthIn(max = DesktopLayout.contentMaxWidth).fillMaxWidth()) {
            item {
                HomeHeader(
                    dailyStreak = dailyStreak,
                    completionRate = completionRate,
                    userName = userName,
                    profilePictureUri = profilePictureUri,
                    date = state.date.toString()
                )
            }

            item(key = "week_strip") {
                DesktopTodayWeekStrip(
                    selectedDate = selectedDate,
                    onDateSelected = onSelectedDateChange,
                    expanded = weekStripExpanded,
                    onToggleExpanded = onToggleWeekStripExpanded,
                    indicatorDates = weekStripIndicatorDates
                )
            }

            // Stage 19e: the inline "New habit" field used to live here — removed now that the
            // Today FAB (Stage 19c) is the single discoverable add entry point, matching
            // Android's HabitListScreen.kt (no inline field there either, only the FAB).
            // "Sync with phone" moved to Settings -> Data Management, matching Android's own
            // placement (Settings -> Data Management -> "Pair with Desktop") instead of living on
            // the Today screen. See DesktopSettingsScreen.kt's DataSectionContent.

            // One combined "Habits" section, as on Android (see habitFrequencySection).
            val uiState = habitsUiState
            habitFrequencySection(
                sectionKey = "habits",
                title = "Habits",
                pending = listOf(HabitFrequency.DAILY, HabitFrequency.WEEKLY, HabitFrequency.MONTHLY)
                    .flatMap { uiState.pendingGroupedHabits[it].orEmpty() },
                completed = uiState.completedHabits,
                expanded = habitsExpanded,
                onToggleExpanded = onToggleHabitsExpanded,
                doneExpanded = habitsDoneExpanded,
                onToggleDone = onToggleHabitsDone,
                onToggleCompletion = habitListViewModel::toggleCompletion,
                onView = { habit -> onSelectHabit(habit.id) },
                onEdit = onEditHabit,
                isDueToday = selectedDate == LocalDate.now()
            )

            remindersEventsItems()
            extraItems()
        }
    }
}

@Composable
private fun HomeHeader(
    dailyStreak: Int,
    completionRate: Int,
    userName: String,
    profilePictureUri: String?,
    date: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = DesktopLayout.screenPadding, end = DesktopLayout.screenPadding, top = DesktopLayout.screenPadding, bottom = DesktopLayout.itemSpacing)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (profilePictureUri != null) {
                    val imageLoader = koinInject<ImageBitmapLoader>()
                    imageLoader.LoadedImage(
                        path = profilePictureUri,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Android's greeting (HabitListScreen's header), fixed for the session like there.
                val greeting = remember {
                    when (LocalDateTime.now().hour) {
                        in 5..11 -> "Good morning"
                        in 12..17 -> "Good afternoon"
                        else -> "Good evening"
                    }
                }
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userName.ifBlank { "Friend" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(DesktopLayout.itemSpacing)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().crystalCardSurface()) {
                Column(
                    modifier = Modifier.padding(DesktopLayout.cardPadding).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "$dailyStreak",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Day streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight().crystalCardSurface()) {
                Column(
                    modifier = Modifier.padding(DesktopLayout.cardPadding).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "$completionRate%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LinearProgressIndicator(
                            progress = { (completionRate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    Text(
                        text = "Done today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
