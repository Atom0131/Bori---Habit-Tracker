package com.apagon.rhythm.ui.habit
import com.apagon.rhythm.core.time.*

import org.koin.compose.viewmodel.koinViewModel

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.apagon.rhythm.core.time.ChronoUnit
import com.apagon.rhythm.core.time.TemporalAdjusters
import kotlinx.coroutines.launch
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.text.format.DateFormat
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.apagon.rhythm.ui.util.*
import com.apagon.rhythm.ui.todos.TodoRow
import coil3.compose.AsyncImage
import com.apagon.rhythm.data.model.ChecklistProgress
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.ui.components.AddOption
import com.apagon.rhythm.ui.components.AddTypePickerSheet
import com.apagon.rhythm.ui.settings.SettingsViewModel
import com.apagon.rhythm.ui.stats.HabitProgressCard
import com.apagon.rhythm.ui.stats.HabitYearStats
import com.apagon.rhythm.ui.stats.StatsHabitSheet
import com.apagon.rhythm.ui.stats.StatsViewModel
import com.apagon.rhythm.ui.todos.AddTodoSheet
import com.apagon.rhythm.ui.todos.TodoRow
import com.apagon.rhythm.ui.todos.TodoViewModel
import com.apagon.rhythm.ui.calendar.AddCalendarEventSheet
import com.apagon.rhythm.ui.reminders.AddReminderSheet
import com.apagon.rhythm.ui.util.CollapsibleSectionHeader
import com.apagon.rhythm.ui.util.HabitCard
import com.apagon.rhythm.ui.util.ProPaywallSheet
import com.apagon.rhythm.ui.util.resolveDisplayColor
import com.apagon.rhythm.ui.util.findActivity
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import com.apagon.rhythm.core.time.YearMonth
import com.apagon.rhythm.core.time.DateTimeFormatter
import com.apagon.rhythm.core.time.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import com.apagon.rhythm.ui.util.resolvedIcon

import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextDecoration
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.model.Reminder
import androidx.compose.ui.text.style.TextOverflow
import com.apagon.rhythm.ui.util.isScheduledForDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HabitListScreen(
    viewModel: HabitListViewModel = koinViewModel(),
    todoViewModel: TodoViewModel = koinViewModel(),
    statsViewModel: StatsViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    onNavigateToSettings: () -> Unit = {},
    pendingHabitId: Long? = null,
    onHabitIdConsumed: () -> Unit = {}
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val displayMonth by viewModel.displayMonth.collectAsState()
    val indicatorDates by viewModel.indicatorDates.collectAsState()
    val calendarEventDates by viewModel.calendarEventDates.collectAsState()
    val habitsUiState by viewModel.habitsUiState.collectAsState()
    val groupedHabits = habitsUiState.groupedHabits
    val todayCompletions = habitsUiState.completions
    val todayChecklistProgress = habitsUiState.checklistProgress
    val tomorrowGroupedHabits by viewModel.tomorrowGroupedHabits.collectAsState()
    val tomorrowHabitIds = remember(tomorrowGroupedHabits, groupedHabits) {
        val currentIds = groupedHabits.values.flatten().map { it.id }.toSet()
        tomorrowGroupedHabits.values.flatten()
            .map { it.id }
            .filter { it !in currentIds }
            .toSet()
    }
    val upcomingHabits by viewModel.upcomingHabits.collectAsState()
    val dailyStreak by viewModel.dailyStreak.collectAsState()
    val completionRate by viewModel.completionRate.collectAsState()
    val pendingTodos by todoViewModel.pendingTodos.collectAsState()
    val overdueTodos by viewModel.selectedDateOverdueTodos.collectAsState()
    val dueTodayTodos by viewModel.selectedDateTodos.collectAsState()
    val completedTodos by viewModel.selectedDateCompletedTodos.collectAsState()

    val selectedDayEvents by viewModel.selectedDayEvents.collectAsState()
    val overdueReminders by viewModel.overdueReminders.collectAsState()
    val completedRemindersToday by viewModel.selectedDateCompletedReminders.collectAsState()
    val selectedDayReminders by viewModel.selectedDayReminders.collectAsState()
    val allActiveReminderCount by viewModel.allActiveReminderCount.collectAsState()
    val selectedDateTodos by viewModel.selectedDateTodos.collectAsState()
    val selectedDateJournalEntries by viewModel.selectedDateJournalEntries.collectAsState()
    val habitStats by statsViewModel.habitStats.collectAsState()
    val calendarMode by settingsViewModel.homeViewCalendar.collectAsState()
    val calendarListMode by settingsViewModel.calendarListMode.collectAsState()
    val availableCalendars by settingsViewModel.availableCalendars.collectAsState()
    val calendarCompletionDates by viewModel.calendarCompletionDates.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val userName by settingsViewModel.userName.collectAsState(initial = "")
    val profilePictureUri by settingsViewModel.profilePictureUri.collectAsState(initial = null)
    var showSheet by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var paywallReason by remember { mutableStateOf<String?>(null) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var viewingHabit by remember { mutableStateOf<Habit?>(null) }
    LaunchedEffect(pendingHabitId, groupedHabits) {
        val id = pendingHabitId ?: return@LaunchedEffect
        groupedHabits.values.flatten().firstOrNull { it.id == id }?.let { habit ->
            viewingHabit = habit
            onHabitIdConsumed()
        }
    }
    var completedExpanded by remember { mutableStateOf(false) }
    var upcomingExpanded by remember { mutableStateOf(false) }
    var showAddTypePicker by remember { mutableStateOf(false) }
    var showAddTodo by remember { mutableStateOf(false) }
    var showAddReminder by remember { mutableStateOf(false) }
    var showAddEventSheet by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var overdueTodosExpanded by remember { mutableStateOf(false) }
    var completedTodosExpanded by remember { mutableStateOf(false) }
    var calendarExpanded by remember { mutableStateOf(false) }
    var selectedProgressStat by remember { mutableStateOf<HabitYearStats?>(null) }
    var showProgressSheet by remember { mutableStateOf(false) }
    val todayListState = rememberLazyListState()

    val completedHabits = habitsUiState.completedHabits
    val pendingDailyHabits = habitsUiState.pendingGroupedHabits[HabitFrequency.DAILY] ?: emptyList()
    val pendingWeeklyHabits = habitsUiState.pendingGroupedHabits[HabitFrequency.WEEKLY] ?: emptyList()
    val pendingMonthlyHabits = habitsUiState.pendingGroupedHabits[HabitFrequency.MONTHLY] ?: emptyList()
    val allCompleted = completedHabits.isNotEmpty() || completedTodos.isNotEmpty()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTypePicker = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { innerPadding ->
        if (calendarMode) {
            CalendarHabitView(
                innerPadding = innerPadding,
                selectedDate = selectedDate,
                displayMonth = displayMonth,
                groupedHabits = groupedHabits,
                todayCompletions = todayCompletions,
                todayChecklistProgress = todayChecklistProgress,
                dailyStreak = dailyStreak,
                completionRate = completionRate,
                calendarCompletionDates = calendarCompletionDates,
                indicatorDates = indicatorDates,
                overdueTodos = overdueTodos,
                dueTodayTodos = dueTodayTodos,
                completedTodos = completedTodos,
                selectedDayEvents = selectedDayEvents,
                overdueReminders = overdueReminders,
                completedRemindersToday = completedRemindersToday,
                selectedDayReminders = selectedDayReminders,
                selectedDateTodos = selectedDateTodos,
                selectedDateJournalEntries = selectedDateJournalEntries,
                habitStats = habitStats,
                calendarListMode = calendarListMode,
                onNavigateToSettings = onNavigateToSettings,
                onToggleListMode = { settingsViewModel.setCalendarListMode(it) },
                onDateSelected = { viewModel.setSelectedDate(it) },
                onMonthChange = { viewModel.setDisplayMonth(it) },
                onToggleCompletion = { habitId, isDone -> viewModel.toggleCompletion(habitId, isDone) },
                onToggleItem = { habitId, item, itemIsDone -> viewModel.toggleItemCompletion(habitId, item, itemIsDone) },
                onArchive = { viewModel.archiveHabit(it) },
                onDeleteHabit = { viewModel.deleteHabit(it) },
                onEdit = { editingHabit = it },
                onView = { viewingHabit = it },
                onTodoToggle = { todoViewModel.toggleCompletion(it) },
                onTodoEdit = { editingTodo = it },
                onTodoDelete = { todoViewModel.deleteTodo(it) },
                onEventEdit = { editingEvent = it },
                onEventDelete = { viewModel.deleteCalendarEvent(it) },
                onReminderEdit = { editingReminder = it },
                onReminderToggle = { viewModel.toggleReminderCompletion(it) },
                onReminderDelete = { viewModel.deleteReminder(it) },
                onStatClick = { selectedProgressStat = it }
            )
        } else {
            // Today view — minimal list layout
            LazyColumn(
                state = todayListState,
                contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                    item(key = "header") {
                        HomeHeader(
                            dailyStreak = dailyStreak,
                            completionRate = completionRate,
                            userName = userName,
                            profilePictureUri = profilePictureUri,
                            onChipClick = { showProgressSheet = true },
                            onTodayClick = { viewModel.setSelectedDate(LocalDate.now()) },
                            onSettingsClick = onNavigateToSettings
                        )
                    }
                    item(key = "week_strip") {
                        WeekStrip(
                            selectedDate = selectedDate,
                            onDateSelected = { viewModel.setSelectedDate(it) },
                            expanded = calendarExpanded,
                            onToggleExpanded = { calendarExpanded = !calendarExpanded },
                            indicatorDates = calendarEventDates
                        )
                    }

                    item(key = "fsi_banner") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val ctx = LocalContext.current
                            val nm = ctx.getSystemService(NotificationManager::class.java)
                            if (!nm.canUseFullScreenIntent()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "Alert cards are disabled",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                "Alarms, timers, and reminders won't pop up",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                        TextButton(onClick = {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                                Uri.parse("package:${ctx.packageName}")
                                            )
                                            PermissionUtils.safeStartActivity(ctx, intent)
                                        }) {
                                            Text("Fix", color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (groupedHabits.values.flatten().isNotEmpty()) {
                        item(key = "swipe_gesture_tutorial") {
                            val tutorialViewModel: TutorialViewModel = koinViewModel()
                            val hasSeenSwipeGestureTutorial by tutorialViewModel.hasSeenSwipeGestureTutorial.collectAsState(initial = true)
                            if (!hasSeenSwipeGestureTutorial) {
                                TutorialCard(
                                    title = "Quick actions",
                                    description = "Want to know how to manage habits faster?",
                                    bullets = listOf(
                                        "Swipe a habit left to delete it",
                                        "Swipe a habit right to archive it (recoverable later)",
                                        "Tap a habit to see details or check it off"
                                    ),
                                    onDismiss = { tutorialViewModel.setHasSeenSwipeGestureTutorial(true) },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Move Events and Reminders here for prominence
                    eventsSection(
                        events = selectedDayEvents,
                        onEventEdit = { editingEvent = it },
                        onEventDelete = { viewModel.deleteCalendarEvent(it) }
                    )

                    // Daily Habits — single-column list
                    if (pendingDailyHabits.isNotEmpty()) {
                        stickyHeader(key = "daily_header") {
                            SectionHeader(frequency = HabitFrequency.DAILY)
                        }
                        items(pendingDailyHabits, key = { it.id }) { habit ->
                            val checklistProgress = todayChecklistProgress[habit.id]
                            HabitRow(
                                habit = habit,
                                isDone = habit.id in todayCompletions,
                                checklistProgress = checklistProgress,
                                onToggle = { viewModel.toggleCompletion(habit.id, habit.id in todayCompletions) },
                                onToggleItem = { item, itemIsDone ->
                                    viewModel.toggleItemCompletion(habit.id, item, itemIsDone)
                                },
                                onArchive = { viewModel.archiveHabit(habit) },
                                onDelete = { viewModel.deleteHabit(habit) },
                                onEdit = { editingHabit = habit },
                                onView = { viewingHabit = habit },
                                modifier = Modifier.animateItem(),
                                isDueToday = selectedDate == LocalDate.now(),
                                isDueTomorrow = habit.id in tomorrowHabitIds
                            )
                        }
                    }

                    // Weekly Habits — single-column list rows
                    if (pendingWeeklyHabits.isNotEmpty()) {
                        stickyHeader(key = "weekly_header") {
                            SectionHeader(frequency = HabitFrequency.WEEKLY)
                        }
                        items(pendingWeeklyHabits, key = { it.id }) { habit ->
                            val checklistProgress = todayChecklistProgress[habit.id]
                            HabitRow(
                                habit = habit,
                                isDone = habit.id in todayCompletions,
                                checklistProgress = checklistProgress,
                                onToggle = { viewModel.toggleCompletion(habit.id, habit.id in todayCompletions) },
                                onToggleItem = { item, itemIsDone ->
                                    viewModel.toggleItemCompletion(habit.id, item, itemIsDone)
                                },
                                onArchive = { viewModel.archiveHabit(habit) },
                                onDelete = { viewModel.deleteHabit(habit) },
                                onEdit = { editingHabit = habit },
                                onView = { viewingHabit = habit },
                                modifier = Modifier.animateItem(),
                                isDueToday = selectedDate == LocalDate.now(),
                                isDueTomorrow = habit.id in tomorrowHabitIds
                            )
                        }
                    }

                    // Monthly Habits — single-column list rows
                    if (pendingMonthlyHabits.isNotEmpty()) {
                        stickyHeader(key = "monthly_header") {
                            SectionHeader(frequency = HabitFrequency.MONTHLY)
                        }
                        items(pendingMonthlyHabits, key = { it.id }) { habit ->
                            val checklistProgress = todayChecklistProgress[habit.id]
                            HabitRow(
                                habit = habit,
                                isDone = habit.id in todayCompletions,
                                checklistProgress = checklistProgress,
                                onToggle = { viewModel.toggleCompletion(habit.id, habit.id in todayCompletions) },
                                onToggleItem = { item, itemIsDone ->
                                    viewModel.toggleItemCompletion(habit.id, item, itemIsDone)
                                },
                                onArchive = { viewModel.archiveHabit(habit) },
                                onDelete = { viewModel.deleteHabit(habit) },
                                onEdit = { editingHabit = habit },
                                onView = { viewingHabit = habit },
                                modifier = Modifier.animateItem(),
                                isDueToday = selectedDate == LocalDate.now(),
                                isDueTomorrow = habit.id in tomorrowHabitIds
                            )
                        }
                    }

                    // Upcoming
                    if (upcomingHabits.isNotEmpty()) {
                        stickyHeader(key = "upcoming_header") {
                            CollapsibleSectionHeader(
                                title = "Upcoming (${upcomingHabits.size})",
                                expanded = upcomingExpanded,
                                onToggle = { upcomingExpanded = !upcomingExpanded }
                            )
                        }
                        if (upcomingExpanded) {
                            items(upcomingHabits, key = { "upcoming_${it.id}" }) { habit ->
                                HabitRow(
                                    habit = habit,
                                    isDone = false,
                                    checklistProgress = null,
                                    onToggle = {},
                                    onToggleItem = { _, _ -> },
                                    onArchive = { viewModel.archiveHabit(habit) },
                                onDelete = { viewModel.deleteHabit(habit) },
                                    onEdit = { editingHabit = habit },
                                    onView = { viewingHabit = habit },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }

                    // Unified Completed Section
                    completedSection(
                        completedHabits = completedHabits,
                        completedTodos = completedTodos,
                        completedReminders = completedRemindersToday,
                        expanded = completedExpanded,
                        onToggle = { completedExpanded = !completedExpanded },
                        todayChecklistProgress = todayChecklistProgress,
                        onHabitToggle = { viewModel.toggleCompletion(it.id, true) },
                        onHabitItemToggle = { habitId, item, itemIsDone -> viewModel.toggleItemCompletion(habitId, item, itemIsDone) },
                        onHabitArchive = { viewModel.archiveHabit(it) },
                        onHabitDelete = { viewModel.deleteHabit(it) },
                        onHabitEdit = { editingHabit = it },
                        onHabitView = { viewingHabit = it },
                        onTodoToggle = { todoViewModel.toggleCompletion(it) },
                        onTodoEdit = { editingTodo = it },
                        onTodoDelete = { todoViewModel.deleteTodo(it) },
                        onReminderToggle = { viewModel.toggleReminderCompletion(it) },
                        onReminderEdit = { editingReminder = it },
                        onReminderDelete = { viewModel.deleteReminder(it) }
                    )

                    // To-do List (Active)
                    todosSection(
                        overdueTodos = overdueTodos,
                        dueTodayTodos = dueTodayTodos,
                        dueTodayReminders = selectedDayReminders,
                        overdueReminders = overdueReminders,
                        overdueTodosExpanded = overdueTodosExpanded,
                        onToggleOverdueExpanded = { overdueTodosExpanded = !overdueTodosExpanded },
                        onTodoToggle = { todoViewModel.toggleCompletion(it) },
                        onTodoEdit = { editingTodo = it },
                        onTodoDelete = { todoViewModel.deleteTodo(it) },
                        onReminderToggle = { viewModel.toggleReminderCompletion(it) },
                        onReminderEdit = { editingReminder = it },
                        onReminderDelete = { viewModel.deleteReminder(it) }
                    )

                }
        }
    }

    if (showAddTypePicker) {
        AddTypePickerSheet(
            onDismiss = { showAddTypePicker = false },
            options = listOf(
                AddOption("habit", "Habit", Icons.Default.Add, "Set goals and build routines"),
                AddOption("todo", "To-Do", Icons.Default.CheckCircle, "Quick tasks and lists"),
                AddOption("reminder", "Reminder", Icons.Default.AccessTime, "One-time alerts"),
                AddOption("event", "Event", Icons.Default.Event, "Calendar appointments")
            ),
            onOptionSelected = { id ->
                when (id) {
                    "habit" -> {
                        if (!isPro && viewModel.habits.value.size >= 7) {
                            paywallReason = "Upgrade to Pro to add more than 7 habits!"
                            showPaywall = true
                        } else {
                            showSheet = true
                        }
                    }
                    "todo" -> {
                        if (!isPro && pendingTodos.size >= 10) {
                            paywallReason = "Upgrade to Pro to add more than 10 to-dos!"
                            showPaywall = true
                        } else {
                            showAddTodo = true
                        }
                    }
                    "reminder" -> {
                        if (!isPro && allActiveReminderCount >= 3) {
                            paywallReason = "Upgrade to Pro to add more than 3 reminders!"
                            showPaywall = true
                        } else {
                            showAddReminder = true
                        }
                    }
                    "event" -> showAddEventSheet = true
                }
            }
        )
    }

    if (showSheet) {
        AddHabitSheet(
            isPro = isPro,
            onDismiss = { showSheet = false },
            onShowPaywall = {
                paywallReason = it
                showPaywall = true
            },
            onSave = { name, description, frequency, weekDaysMask, monthDaysMask, isChecklist, checklistItems, colorIndex, colorArgb, durationDays, iconIndex, reminderTime, soundUri, vibrationPatternId ->
                viewModel.addHabit(name, description, frequency, weekDaysMask, monthDaysMask, isChecklist, checklistItems, colorIndex, colorArgb, durationDays, iconIndex, reminderTime, soundUri, vibrationPatternId)
                showSheet = false
            }
        )
    }

    viewingHabit?.let { habit ->
        val viewingProgress = todayChecklistProgress[habit.id]
        val isDone = habit.id in todayCompletions
        val streakDays by remember(habit.id) {
            viewModel.getStreakForHabit(habit.id)
        }.collectAsState(initial = 0)
        HabitDetailSheet(
            habit = habit,
            checklistProgress = viewingProgress,
            isDone = isDone,
            streakDays = streakDays,
            onDismiss = { viewingHabit = null },
            onToggleItem = { item, itemIsDone ->
                viewModel.toggleItemCompletion(habit.id, item, itemIsDone)
            },
            onComplete = {
                viewModel.toggleCompletion(habit.id, isDone)
                viewingHabit = null
            },
            onEdit = { editingHabit = habit },
            onSetReminder = { viewModel.updateHabitReminder(habit, it) }
        )
    }

    editingHabit?.let { habit ->
        val editingItems by remember(habit.id) {
            if (habit.isChecklist) viewModel.getItemsForHabit(habit.id)
            else flowOf(emptyList())
        }.collectAsState(initial = emptyList())

        AddHabitSheet(
            initialHabit = habit,
            initialItems = editingItems,
            isPro = isPro,
            onDismiss = { editingHabit = null },
            onShowPaywall = {
                paywallReason = it
                showPaywall = true
            },
            onSave = { name, description, frequency, weekDaysMask, monthDaysMask, isChecklist, checklistItems, colorIndex, colorArgb, durationDays, iconIndex, reminderTime, soundUri, vibrationPatternId ->
                viewModel.updateHabit(habit, name, description, frequency, weekDaysMask, monthDaysMask, isChecklist, checklistItems, colorIndex, colorArgb, durationDays, iconIndex, reminderTime, soundUri, vibrationPatternId)
                editingHabit = null
            }
        )
    }

    if (showAddTodo) {
        AddTodoSheet(
            onDismiss = { showAddTodo = false },
            onSave = { title, note, dueDate, priority, iconIndex, soundUri, vibrationPatternId ->
                todoViewModel.addTodo(title, note, dueDate, priority, iconIndex, soundUri, vibrationPatternId)
                showAddTodo = false
            }
        )
    }

    if (showAddReminder) {
        AddReminderSheet(
            onDismiss = { showAddReminder = false },
            onSave = { title, note, dateTime, soundUri ->
                viewModel.addReminder(title, note, dateTime, soundUri)
                showAddReminder = false
            }
        )
    }

    LaunchedEffect(showAddEventSheet) {
        if (showAddEventSheet) settingsViewModel.refreshAvailableCalendars()
    }

    if (showAddEventSheet) {
        AddCalendarEventSheet(
            initialDate = selectedDate,
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

    editingTodo?.let { todo ->
        AddTodoSheet(
            existing = todo,
            onDismiss = { editingTodo = null },
            onSave = { title, note, dueDate, priority, iconIndex, soundUri, vibrationPatternId ->
                todoViewModel.updateTodo(todo, title, note, dueDate, priority, iconIndex, soundUri, vibrationPatternId)
                editingTodo = null
            }
        )
    }

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

    if (showProgressSheet) {
        YourProgressSheet(
            habitStats = habitStats,
            onDismiss = { showProgressSheet = false },
            onStatClick = { stat ->
                showProgressSheet = false
                selectedProgressStat = stat
            }
        )
    }

    selectedProgressStat?.let { stat ->
        StatsHabitSheet(
            stat = stat,
            onDismiss = { selectedProgressStat = null }
        )
    }

    val context = LocalContext.current
    if (showPaywall) {
        ProPaywallSheet(
            reason = paywallReason,
            onDismiss = {
                showPaywall = false
                paywallReason = null
            },
            onUpgrade = { productId ->
                val activity = context.findActivity()
                if (activity != null) {
                    viewModel.startBillingFlow(productId)
                }
                showPaywall = false
                paywallReason = null
            }
        )
    }
}

@Composable
private fun HomeHeader(
    dailyStreak: Int,
    completionRate: Int,
    userName: String,
    profilePictureUri: String?,
    onChipClick: () -> Unit,
    onTodayClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePictureUri != null) {
                        AsyncImage(
                            model = profilePictureUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (userName.isBlank()) "Friend" else userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTodayClick) {
                    Icon(
                        Icons.Default.Today,
                        contentDescription = "Go to Today",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.weight(1f).clickable { onChipClick() }
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    Text(
                        text = "Done today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
