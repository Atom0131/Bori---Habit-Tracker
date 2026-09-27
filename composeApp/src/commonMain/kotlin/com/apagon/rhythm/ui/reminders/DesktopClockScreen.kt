package com.apagon.rhythm.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.ui.alarms.AlarmViewModel
import com.apagon.rhythm.ui.alarms.DesktopAddAlarmSheet
import com.apagon.rhythm.ui.alarms.DesktopAddTimerSheet
import com.apagon.rhythm.ui.alarms.TimerViewModel
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import org.koin.compose.viewmodel.koinViewModel

// Desktop counterpart to androidMain's ClockScreen.kt (Stage 12) — the
// largest file in the original survey (1445 lines: NotificationManager
// permission banners, exact-alarm settings deep-links, wake-up-check puzzle
// hookup). None of that exists on desktop, so this is a from-scratch plain-M3
// screen: a sub-TabRow over Alarms/Timers/Reminders, reusing the already
// commonMain AlarmViewModel/ReminderViewModel/TimerViewModel as-is.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopClockScreen() {
    var section by remember { mutableIntStateOf(0) }
    val sections = listOf("Alarms", "Timers", "Reminders")

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = { TopAppBar(title = { Text("Clock") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = section) {
                sections.forEachIndexed { index, label ->
                    Tab(selected = section == index, onClick = { section = index }, text = { Text(label) })
                }
            }
            when (section) {
                0 -> DesktopAlarmsSection()
                1 -> DesktopTimersSection()
                2 -> DesktopRemindersSection()
            }
        }
    }
}

@Composable
private fun DesktopAlarmsSection(viewModel: AlarmViewModel = koinViewModel()) {
    val alarms by viewModel.alarms.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { showAdd = true }) { Text("+ Alarm") }
        if (alarms.isEmpty()) {
            Text("No alarms yet.", modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alarms, key = { it.id }) { alarm ->
                    Box(Modifier.fillMaxWidth().crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("%02d:%02d".format(alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    if (alarm.label.isNotBlank()) alarm.label else if (alarm.repeatDaysMask == 0) "One-time" else "Repeats weekly",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = alarm.isEnabled, onCheckedChange = { viewModel.toggleEnabled(alarm) })
                            TextButton(onClick = { viewModel.deleteAlarm(alarm) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        DesktopAddAlarmSheet(
            onDismiss = { showAdd = false },
            onSave = { label, hour, minute, repeatDaysMask ->
                viewModel.addAlarm(label, hour, minute, repeatDaysMask, soundUri = "")
                showAdd = false
            }
        )
    }
}

@Composable
private fun DesktopTimersSection(viewModel: TimerViewModel = koinViewModel()) {
    val timerStates by viewModel.timerUiStates.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { showAdd = true }) { Text("+ Timer") }
        if (timerStates.isEmpty()) {
            Text("No timers yet.", modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(timerStates, key = { it.timer.id }) { state ->
                    val timer = state.timer
                    Box(Modifier.fillMaxWidth().crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(timer.label.ifBlank { "Timer" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    state.pomoPhaseLabel?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(formatDuration(state.displayRemaining), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            }
                            if (timer.durationSeconds > 0) {
                                LinearProgressIndicator(
                                    progress = { 1f - (state.displayRemaining.toFloat() / timer.durationSeconds.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                )
                            }
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (state.isRunning) {
                                    TextButton(onClick = { viewModel.pauseTimer(timer) }) { Text("Pause") }
                                } else {
                                    TextButton(onClick = { viewModel.startTimer(timer) }) { Text("Start") }
                                }
                                TextButton(onClick = { viewModel.resetTimer(timer) }) { Text("Reset") }
                                TextButton(onClick = { viewModel.deleteTimer(timer) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        DesktopAddTimerSheet(
            onDismiss = { showAdd = false },
            onSave = { label, durationSeconds ->
                viewModel.addTimer(label, durationSeconds)
                showAdd = false
            },
            onSavePomo = { label, workMin, shortBreakMin, longBreakMin, sessions ->
                viewModel.addPomoTimer(label, workMin * 60, shortBreakMin * 60, longBreakMin * 60, sessions)
                showAdd = false
            }
        )
    }
}

@Composable
private fun DesktopRemindersSection(viewModel: ReminderViewModel = koinViewModel()) {
    val upcoming by viewModel.upcomingReminders.collectAsState()
    val past by viewModel.pastReminders.collectAsState()
    val completed by viewModel.completedReminders.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { showAdd = true }) { Text("+ Reminder") }
        val all = upcoming + past + completed
        if (all.isEmpty()) {
            Text("No reminders yet.", modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(all, key = { it.id }) { reminder ->
                    DesktopReminderRow(reminder, onToggle = { viewModel.toggleCompletion(reminder) }, onDelete = { viewModel.deleteReminder(reminder) })
                }
            }
        }
    }

    if (showAdd) {
        DesktopAddReminderSheet(
            onDismiss = { showAdd = false },
            onSave = { title, note, dateTime ->
                viewModel.addReminder(title, note, dateTime)
                showAdd = false
            }
        )
    }
}

@Composable
private fun DesktopReminderRow(reminder: Reminder, onToggle: () -> Unit, onDelete: () -> Unit) {
    Box(Modifier.fillMaxWidth().crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
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

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
