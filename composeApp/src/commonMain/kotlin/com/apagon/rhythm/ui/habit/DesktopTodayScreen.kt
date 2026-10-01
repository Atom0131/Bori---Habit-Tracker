package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import com.apagon.rhythm.data.model.TodoPriority
import com.apagon.rhythm.ui.calendar.DesktopTodayCalendarContent
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.settings.DesktopSettingsViewModel
import com.apagon.rhythm.ui.todos.TodoViewModel
import com.apagon.rhythm.ui.util.RhythmAddFab
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import com.apagon.rhythm.ui.util.RhythmDropdownMenu
import org.koin.compose.viewmodel.koinViewModel

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
 * FAB opening a Habit/To-do AddTypePickerSheet. Desktop has no bottom-sheet UX, so the picker here
 * is a RhythmDropdownMenu off the FAB instead, each item opening a small RhythmAlertDialog (no
 * desktop "add habit" sheet existed yet — the inline fields in DesktopHabitScreen.kt/
 * DesktopTodoScreen.kt are the only other entry points, and Stage 19e tracks demoting those once
 * this one is confirmed working). Calendar mode keeps its own add-event FAB
 * (DesktopTodayCalendarContent), so this FAB only shows in list mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopTodayScreen(
    settingsViewModel: DesktopSettingsViewModel = koinViewModel(),
    habitViewModel: DesktopHabitViewModel = koinViewModel(),
    todoViewModel: TodoViewModel = koinViewModel()
) {
    val calendarMode by settingsViewModel.homeViewCalendar.collectAsState()
    var showAddMenu by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showAddTodoDialog by remember { mutableStateOf(false) }

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
                Box {
                    RhythmAddFab(onClick = { showAddMenu = true })
                    RhythmDropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Habit") },
                            onClick = { showAddMenu = false; showAddHabitDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("To-do") },
                            onClick = { showAddMenu = false; showAddTodoDialog = true }
                        )
                    }
                }
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

    if (showAddHabitDialog) {
        var name by remember { mutableStateOf("") }
        RhythmAlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            title = { Text("New habit") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
            },
            dismissButton = {
                TextButton(onClick = { showAddHabitDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        habitViewModel.addHabit(name)
                        showAddHabitDialog = false
                    }
                ) { Text("Add") }
            }
        )
    }

    if (showAddTodoDialog) {
        var title by remember { mutableStateOf("") }
        RhythmAlertDialog(
            onDismissRequest = { showAddTodoDialog = false },
            title = { Text("New to-do") },
            text = {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
            },
            dismissButton = {
                TextButton(onClick = { showAddTodoDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        todoViewModel.addTodo(title = title, note = "", dueDate = "", priority = TodoPriority.NONE)
                        showAddTodoDialog = false
                    }
                ) { Text("Add") }
            }
        )
    }
}
