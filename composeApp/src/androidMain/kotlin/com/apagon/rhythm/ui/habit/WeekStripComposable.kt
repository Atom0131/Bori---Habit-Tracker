package com.apagon.rhythm.ui.habit
import com.apagon.rhythm.core.time.*

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.apagon.rhythm.ui.util.PermissionUtils
import com.apagon.rhythm.ui.util.resolveDisplayColor
import com.apagon.rhythm.ui.util.resolvedIcon
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.YearMonth
import com.apagon.rhythm.core.time.DateTimeFormatter
import com.apagon.rhythm.core.time.TextStyle
import com.apagon.rhythm.core.time.ChronoUnit
import com.apagon.rhythm.core.time.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarHabitView(
    innerPadding: PaddingValues,
    selectedDate: LocalDate,
    displayMonth: YearMonth,
    groupedHabits: Map<HabitFrequency, List<Habit>>,
    todayCompletions: Set<Long>,
    todayChecklistProgress: Map<Long, ChecklistProgress>,
    dailyStreak: Int,
    completionRate: Int,
    calendarCompletionDates: Set<LocalDate>,
    indicatorDates: Set<String>,
    overdueTodos: List<Todo>,
    dueTodayTodos: List<Todo>,
    completedTodos: List<Todo>,
    selectedDayEvents: List<CalendarEvent>,
    overdueReminders: List<Reminder>,
    completedRemindersToday: List<Reminder>,
    selectedDayReminders: List<Reminder>,
    selectedDateTodos: List<Todo>,
    selectedDateJournalEntries: List<JournalEntry>,
    habitStats: List<HabitYearStats>,
    calendarListMode: Boolean,
    onNavigateToSettings: () -> Unit,
    onToggleListMode: (Boolean) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
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
    val today = remember { LocalDate.now() }
    var selectedDayForDetail by remember { mutableStateOf<LocalDate?>(null) }

    val historyByDate = remember(habitStats) {
        val map = mutableMapOf<LocalDate, MutableList<Habit>>()
        habitStats.forEach { stat ->
            stat.completedDates.forEach { dateStr ->
                try {
                    val date = LocalDate.parse(dateStr)
                    map.getOrPut(date) { mutableListOf() }.add(stat.habit)
                } catch (e: Exception) {}
            }
        }
        map
    }

    BackHandler(enabled = selectedDayForDetail != null) { selectedDayForDetail = null }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Layer 1: Full month calendar grid ──────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
        ) {
            // Top app bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Habit Calendar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onToggleListMode(!calendarListMode) }) {
                        Icon(
                            if (calendarListMode) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "Switch view mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Full-screen-intent permission warning (Android 14+) — same check as ClockScreen/HabitListScreen's
            // Today view, so Calendar-view users see it too instead of it only appearing in one view mode.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val ctx = LocalContext.current
                val nm = ctx.getSystemService(NotificationManager::class.java)
                if (!nm.canUseFullScreenIntent()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Alarms won't wake the screen",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Grant full-screen intent permission so alarms can show on the lock screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            TextButton(onClick = {
                                val intent = Intent(
                                    "android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT",
                                    Uri.parse("package:${ctx.packageName}")
                                )
                                PermissionUtils.safeStartActivity(ctx, intent)
                            }) {
                                Text("Open Settings", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // Stats chips — pinned directly below top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "🔥 $dailyStreak Day Streak",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Card(
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "✓ $completionRate% done today",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Month navigation row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(displayMonth.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    text = displayMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { onMonthChange(displayMonth.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
                val context = LocalContext.current

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                onDateSelected(today)
                                onMonthChange(YearMonth.now())
                                if (calendarListMode) {
                                    selectedDayForDetail = today
                                }
                            },
                            onLongClick = {
                                // Haptic feedback
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    vibrator?.vibrate(50)
                                }

                                // Toggle list mode
                                onToggleListMode(true)
                                onDateSelected(today)
                                onMonthChange(YearMonth.now())
                                selectedDayForDetail = today
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Today,
                        contentDescription = "Go to Today / Hold for List View",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {

            if (calendarListMode) {
                HistoryListView(
                    displayMonth = displayMonth,
                    historyByDate = historyByDate,
                    onDateSelected = onDateSelected,
                    onShowDetail = { selectedDayForDetail = it }
                )
            } else {
                // Floating calendar card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .pointerInput(displayMonth) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    totalDrag += amount
                                },
                                onDragEnd = {
                                    val threshold = 50.dp.toPx()
                                    when {
                                        totalDrag < -threshold -> onMonthChange(displayMonth.plusMonths(1))
                                        totalDrag >  threshold -> onMonthChange(displayMonth.minusMonths(1))
                                    }
                                    totalDrag = 0f
                                }
                            )
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Day-of-week header M T W T F S S
                        val dowOrdered = remember {
                            val all = DayOfWeek.values()
                            val sundayIdx = all.indexOfFirst { it == DayOfWeek.SUNDAY }
                            all.drop(sundayIdx) + all.take(sundayIdx)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            dowOrdered.forEach { dow ->
                                Text(
                                    text = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Month grid — cells precomputed once per month (not per recomposition) and
                        // colors hoisted once above the loop, same fix pattern as IconPickerModalSheet.
                        val monthCells = remember(displayMonth) { buildMonthCells(displayMonth) }
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
                        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                        val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

                        Column(modifier = Modifier.fillMaxWidth()) {
                            monthCells.chunked(7).forEach { weekCells ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    weekCells.forEach { cell ->
                                        val isToday = cell.isCurrentMonth && cell.date == today
                                        val isSelected = cell.isCurrentMonth && cell.date == selectedDayForDetail
                                        val hasDot = cell.isCurrentMonth && indicatorDates.contains(cell.dateStr)
                                        MonthDayCell(
                                            dayNumber = cell.dayNumber,
                                            isCurrentMonth = cell.isCurrentMonth,
                                            isToday = isToday,
                                            isSelected = isSelected,
                                            hasDot = hasDot,
                                            primaryColor = primaryColor,
                                            onPrimaryColor = onPrimaryColor,
                                            onSurfaceColor = onSurfaceColor,
                                            onSurfaceVariantColor = onSurfaceVariantColor,
                                            onClick = {
                                                onDateSelected(cell.date)
                                                onMonthChange(YearMonth.from(cell.date))
                                                selectedDayForDetail = cell.date
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Hint text
                        Text(
                            text = "Tap a day to see habits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            } // centering Box
        }

        // ── Layer 2: Day Detail overlay ────────────────────────────────────
        AnimatedVisibility(
            visible = selectedDayForDetail != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            DayDetailView(
                innerPadding = innerPadding,
                date = selectedDayForDetail ?: selectedDate,
                groupedHabits = groupedHabits,
                todayCompletions = todayCompletions,
                todayChecklistProgress = todayChecklistProgress,
                overdueTodos = overdueTodos,
                dueTodayTodos = dueTodayTodos,
                completedTodos = completedTodos,
                selectedDayEvents = selectedDayEvents,
                overdueReminders = overdueReminders,
                completedRemindersToday = completedRemindersToday,
                selectedDayReminders = selectedDayReminders,
                selectedDateTodos = selectedDateTodos,
                journalEntries = selectedDateJournalEntries,
                habitStats = habitStats,
                onBack = { selectedDayForDetail = null },
                onToggleCompletion = onToggleCompletion,
                onToggleItem = onToggleItem,
                onArchive = onArchive,
                onDeleteHabit = onDeleteHabit,
                onEdit = onEdit,
                onView = onView,
                onTodoToggle = onTodoToggle,
                onTodoEdit = onTodoEdit,
                onTodoDelete = onTodoDelete,
                onEventEdit = onEventEdit,
                onEventDelete = onEventDelete,
                onReminderEdit = onReminderEdit,
                onReminderToggle = onReminderToggle,
                onReminderDelete = onReminderDelete,
                onStatClick = onStatClick
            )
        }
    }
}

@Composable
fun HistoryListView(
    displayMonth: YearMonth,
    historyByDate: Map<LocalDate, List<Habit>>,
    onDateSelected: (LocalDate) -> Unit,
    onShowDetail: (LocalDate) -> Unit
) {
    val daysInMonth = displayMonth.lengthOfMonth()
    val today = LocalDate.now()
    val days = remember(displayMonth) {
        (1..daysInMonth).map { displayMonth.atDay(it) }
            .filter { !it.isAfter(today) }
            .reversed()
    }

    if (days.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No past days in this month yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(days, key = { it.toString() }) { date ->
                HistoryDayCard(
                    date = date,
                    isToday = date == today,
                    completedHabits = historyByDate[date] ?: emptyList(),
                    onClick = {
                        onDateSelected(date)
                        onShowDetail(date)
                    }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun HistoryDayCard(
    date: LocalDate,
    isToday: Boolean,
    completedHabits: List<Habit>,
    onClick: () -> Unit
) {
    val dateLabel = remember(date) {
        date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                             else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isToday) "Today, $dateLabel" else dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (completedHabits.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${completedHabits.size} done",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (completedHabits.isEmpty()) {
                Text(
                    "No habits completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    completedHabits.take(6).forEach { habit ->
                        val habitColor = resolveDisplayColor(habit.colorIndex, habit.colorArgb)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(habitColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = habit.resolvedIcon(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (completedHabits.size > 6) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+${completedHabits.size - 6}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    indicatorDates: Set<String> = emptySet()
) {
    val today = remember { LocalDate.now() }
    val initialPage = 500 // Arbitrary large number for infinite-like scrolling
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 1000 })
    val coroutineScope = rememberCoroutineScope()

    // Sync pager to selectedDate when it changes externally
    LaunchedEffect(selectedDate) {
        val startOfSelectedWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val startOfTodayWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekOffset = ChronoUnit.WEEKS.between(startOfTodayWeek, startOfSelectedWeek).toInt()
        val targetPage = initialPage + weekOffset
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    var displayedMonth by remember(selectedDate) { mutableStateOf(selectedDate.withDayOfMonth(1)) }

    LaunchedEffect(selectedDate) {
        displayedMonth = selectedDate.withDayOfMonth(1)
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "chevron_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(expanded) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 20f && !expanded) onToggleExpanded()
                    else if (dragAmount < -20f && expanded) onToggleExpanded()
                }
            }
    ) {
        // Week strip row with navigation arrows and expand hint chevron
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = !expanded,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Week",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        userScrollEnabled = true
                    ) { page ->
                        val weekOffset = page - initialPage
                        val startOfPagerWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).plusWeeks(weekOffset.toLong())

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (0..6).forEach { offset ->
                                val date = startOfPagerWeek.plusDays(offset.toLong())
                                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                DayChip(
                                    date = date,
                                    isSelected = date == selectedDate,
                                    isToday = date == today,
                                    hasIndicator = dateStr in indicatorDates,
                                    onClick = { onDateSelected(date) }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Week",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (expanded) {
                Spacer(Modifier.weight(1f))
            }
            IconButton(
                onClick = onToggleExpanded,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse calendar" else "Expand calendar",
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // Full month calendar grid
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                // tween, not spring — matches the chevron's own spec above and the exit below,
                // instead of a slow overshoot-prone spring competing with the pager's animation.
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top
            ) + fadeIn(tween(200)),
            exit = shrinkVertically(
                animationSpec = tween(250, easing = FastOutLinearInEasing),
                shrinkTowards = Alignment.Top
            ) + fadeOut(tween(200))
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Month navigation header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous month",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next month",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            onDateSelected(today)
                            displayedMonth = today.withDayOfMonth(1)
                        }) {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = "Go to Today",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Day-of-week header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Month day grid
                    val calendarWeeks = remember(displayedMonth) {
                        val firstDay = displayedMonth
                        val startOffset = firstDay.dayOfWeek.value % 7 // Sun = 0
                        val daysInMonth = firstDay.lengthOfMonth()
                        val totalWeeks = (startOffset + daysInMonth + 6) / 7

                        (0 until totalWeeks).map { week ->
                            (0..6).map { dow ->
                                val dayIndex = week * 7 + dow - startOffset + 1
                                if (dayIndex in 1..daysInMonth) {
                                    firstDay.withDayOfMonth(dayIndex)
                                } else {
                                    null
                                }
                            }
                        }
                    }

                    for (weekDays in calendarWeeks) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            for (date in weekDays) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (date != null) {
                                        DayChip(
                                            date = date,
                                            isSelected = date == selectedDate,
                                            isToday = date == today,
                                            hasIndicator = date.format(DateTimeFormatter.ISO_LOCAL_DATE) in indicatorDates,
                                            showDayLabel = false,
                                            onClick = {
                                                onDateSelected(date)
                                                onToggleExpanded()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One cell of [CalendarHabitView]'s month grid, precomputed once per [buildMonthCells] call
 * instead of being recomputed inline on every recomposition of the grid. */
private data class MonthCell(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val dayNumber: Int,
    val dateStr: String
)

private fun buildMonthCells(displayMonth: YearMonth): List<MonthCell> {
    val firstOfMonth = displayMonth.atDay(1)
    val startOffset = firstOfMonth.dayOfWeek.value % 7
    val daysInMonth = displayMonth.lengthOfMonth()
    val rows = (startOffset + daysInMonth + 6) / 7
    val prevMonth = displayMonth.minusMonths(1)
    val nextMonth = displayMonth.plusMonths(1)
    val prevMonthDays = prevMonth.lengthOfMonth()

    return (0 until rows * 7).map { cellIndex ->
        val (cellDate, isCurrentMonth) = when {
            cellIndex < startOffset ->
                prevMonth.atDay(prevMonthDays - startOffset + cellIndex + 1) to false
            cellIndex < startOffset + daysInMonth ->
                firstOfMonth.withDayOfMonth(cellIndex - startOffset + 1) to true
            else ->
                nextMonth.atDay(cellIndex - startOffset - daysInMonth + 1) to false
        }
        MonthCell(
            date = cellDate,
            isCurrentMonth = isCurrentMonth,
            dayNumber = cellDate.dayOfMonth,
            dateStr = if (isCurrentMonth) cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE) else ""
        )
    }
}

/** Extracted so Compose can skip-recompose individual unchanged cells instead of the whole grid
 * recomposing as one inline block — same fix as [DayChip] already gets, applied to the month grid. */
@Composable
private fun RowScope.MonthDayCell(
    dayNumber: Int,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    hasDot: Boolean,
    primaryColor: Color,
    onPrimaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) primaryColor else Color.Transparent
    val borderMod = if (isToday && !isSelected)
        Modifier.border(1.dp, primaryColor.copy(alpha = 0.5f), CircleShape)
    else Modifier
    val textColor = when {
        !isCurrentMonth -> onSurfaceVariantColor.copy(alpha = 0.4f)
        isSelected      -> onPrimaryColor
        isToday         -> primaryColor
        else            -> onSurfaceColor
    }
    val dotColor = if (isSelected) onPrimaryColor else primaryColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
            .then(borderMod)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                textAlign = TextAlign.Center
            )
            if (hasDot) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            } else {
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun DayChip(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasIndicator: Boolean = false,
    showDayLabel: Boolean = true,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday    -> MaterialTheme.colorScheme.primaryContainer
        else       -> Color.Transparent
    }
    val labelColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday    -> MaterialTheme.colorScheme.onPrimaryContainer
        else       -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = if (showDayLabel) 6.dp else 8.dp)
            .width(36.dp)
    ) {
        if (showDayLabel) {
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Text(
            text = date.dayOfMonth.toString(),
            style = if (showDayLabel) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
            color = labelColor,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
        )
        if (hasIndicator) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
