package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.apagon.rhythm.ui.calendar.DesktopAddCalendarEventSheet
import com.apagon.rhythm.ui.calendar.DesktopCalendarViewModel
import com.apagon.rhythm.ui.calendar.DesktopTodayCalendarContent
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.reminders.DesktopAddReminderSheet
import com.apagon.rhythm.ui.reminders.ReminderViewModel
import com.apagon.rhythm.ui.settings.DesktopSettingsViewModel
import com.apagon.rhythm.ui.todos.DesktopAddTodoSheet
import com.apagon.rhythm.ui.todos.TodoViewModel
import com.apagon.rhythm.ui.util.RhythmAddFab
import com.apagon.rhythm.ui.util.RhythmDropdownMenu
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.Icons
import com.apagon.rhythm.ui.components.AddTypePickerSheet
import com.apagon.rhythm.ui.components.AddOption

/**
 * Stage 17e: the desktop sidebar's "Today" entry — merges what were three separate sidebar
 * screens (Habits, To-dos, Calendar) into one, mirroring the real Android app exactly: its
 * bottom nav has no separate Habits/To-dos/Calendar tabs at all — MainActivity.kt's "list" ("Today")
 * destination directly composes TodoViewModel state alongside habits, with a calendarMode toggle
 * (the homeViewCalendar setting, already ported to DesktopSettingsViewModel — previously only the
 * Settings screen's own toggle consumed it; this is its first real consumer) swapping in
 * CalendarHabitView. This is the one Scaffold/TopAppBar for both modes; List mode is
 * DesktopTodayListContent (habit sections + a to-do section in one LazyColumn, Stage 15e's
 * habit-detail-as-pane still intact), Calendar mode is DesktopTodayCalendarContent (month grid +
 * day-detail, day-detail now including Reminders — moved off Clock in Stage 17d/17e to match
 * mobile's DayDetailView.kt).
 *
 * Stage 19c: list mode previously had no persistent add affordance at all (the only way to add a
 * habit or to-do was scrolling to their inline fields) — Android's HabitListScreen.kt has a single
 * FAB opening an AddTypePickerSheet with four peer options (Habit/To-Do/Reminder/Event,
 * HabitListScreen.kt:668-707). The type picker is the same AddTypePickerSheet (a
 * dropdown until 2026-10-09). What opens after picking a type is a real sheet for all
 * four now: Habit/To-do open [DesktopAddHabitSheet]/[DesktopAddTodoSheet] (full field coverage —
 * schedule/color/icon/checklist/reminder for habits, icon/due-date/priority for to-dos — replacing
 * the name-only `RhythmAlertDialog`s this screen used through Stage 19h). Reminder/Event reuse the
 * existing DesktopAddReminderSheet/DesktopAddCalendarEventSheet already used by Calendar mode's
 * day-detail "+Reminder" button and add-event FAB. Calendar mode keeps its own add-event FAB
 * (DesktopTodayCalendarContent), so this FAB only shows in list mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopTodayScreen(
    settingsViewModel: DesktopSettingsViewModel = koinViewModel(),
    habitViewModel: DesktopHabitViewModel = koinViewModel(),
    todoViewModel: TodoViewModel = koinViewModel(),
    reminderViewModel: ReminderViewModel = koinViewModel(),
    calendarViewModel: DesktopCalendarViewModel = koinViewModel()
) {
    val calendarMode by settingsViewModel.homeViewCalendar.collectAsState()
    var showAddMenu by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var showAddReminder by remember { mutableStateOf(false) }
    var showAddEventSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = {
            TopAppBar(
                title = { Text(if (calendarMode) "Calendar" else "Today", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { settingsViewModel.setHomeViewCalendar(false) }) {
                        Text("List", fontWeight = if (!calendarMode) FontWeight.Bold else FontWeight.Normal)
                    }
                    TextButton(onClick = { settingsViewModel.setHomeViewCalendar(true) }) {
                        Text("Calendar", fontWeight = if (calendarMode) FontWeight.Bold else FontWeight.Normal)
                    }
                },
                colors = crystalTopAppBarColors()
            )
        },
        floatingActionButton = {
            if (!calendarMode) {
                RhythmAddFab(onClick = { showAddMenu = true })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (calendarMode) {
                DesktopTodayCalendarContent()
            } else {
                DesktopTodayListContent()
            }
        }
    }

    if (showAddMenu) {
        AddTypePickerSheet(
            onDismiss = { showAddMenu = false },
            options = listOf(
                AddOption("habit", "Habit", Icons.Default.Add, "Set goals and build routines"),
                AddOption("todo", "To-Do", Icons.Default.CheckCircle, "Quick tasks and lists"),
                AddOption("reminder", "Reminder", Icons.Default.AccessTime, "One-time alerts"),
                AddOption("event", "Event", Icons.Default.Event, "Calendar appointments")
            ),
            onOptionSelected = { id ->
                when (id) {
                    "habit" -> showAddHabitDialog = true
                    "todo" -> showAddTodoDialog = true
                    "reminder" -> showAddReminder = true
                    "event" -> showAddEventSheet = true
                }
            }
        )
    }

    if (showAddHabitDialog) {
        DesktopAddHabitSheet(
            onDismiss = { showAddHabitDialog = false },
            onSave = { name, description, frequency, weekDaysMask, monthDaysMask, isChecklist, checklistItems, colorIndex, colorArgb, durationDays, iconIndex, reminderTime ->
                habitViewModel.addHabit(
                    name = name,
                    description = description,
                    frequency = frequency,
                    weekDaysMask = weekDaysMask,
                    monthDaysMask = monthDaysMask,
                    isChecklist = isChecklist,
                    checklistItems = checklistItems,
                    colorIndex = colorIndex,
                    colorArgb = colorArgb,
                    durationDays = durationDays,
                    iconIndex = iconIndex,
                    reminderTime = reminderTime
                )
                showAddHabitDialog = false
            }
        )
    }

    if (showAddTodoDialog) {
        DesktopAddTodoSheet(
            onDismiss = { showAddTodoDialog = false },
            onSave = { title, note, dueDate, priority, iconIndex ->
                todoViewModel.addTodo(title = title, note = note, dueDate = dueDate, priority = priority, iconIndex = iconIndex)
                showAddTodoDialog = false
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

    if (showAddEventSheet) {
        DesktopAddCalendarEventSheet(
            onDismiss = { showAddEventSheet = false },
            onSave = { event ->
                calendarViewModel.addCalendarEvent(event)
                showAddEventSheet = false
            }
        )
    }
}
