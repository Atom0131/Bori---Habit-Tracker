package com.apagon.rhythm.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.ui.alarms.AlarmViewModel
import com.apagon.rhythm.ui.alarms.DesktopAddAlarmSheet
import com.apagon.rhythm.ui.alarms.DesktopAddTimerSheet
import com.apagon.rhythm.ui.alarms.TimerUiState
import com.apagon.rhythm.ui.alarms.TimerViewModel
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalSwitchColors
import com.apagon.rhythm.ui.util.CollapsibleSectionHeader
import com.apagon.rhythm.ui.util.SectionHeaderTier
import org.koin.compose.viewmodel.koinViewModel

// Desktop counterpart to androidMain's ClockScreen.kt (Stage 12), rewritten in the layout-parity
// round to match the phone app's single stacked "Schedule" page instead of a sub-TabRow — Android's
// ClockScreen.kt (L67-241) drops Scaffold's tab pattern in favor of one scrollable page combining
// Alarms/Timers/Reminders. This port keeps that single-page shape but deliberately does NOT port the
// phone's responsive card grid, tap-to-view detail sheets, or unified add-type picker — desktop's
// existing row-style cards and per-section inline "+" buttons stay as they are; only the tab
// structure changes.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopClockScreen(
    alarmViewModel: AlarmViewModel = koinViewModel(),
    timerViewModel: TimerViewModel = koinViewModel(),
    reminderViewModel: ReminderViewModel = koinViewModel()
) {
    val alarms by alarmViewModel.alarms.collectAsState()
    val timerStates by timerViewModel.timerUiStates.collectAsState()
    val upcoming by reminderViewModel.upcomingReminders.collectAsState()
    val past by reminderViewModel.pastReminders.collectAsState()
    val completed by reminderViewModel.completedReminders.collectAsState()

    var showAddAlarm by remember { mutableStateOf(false) }
    var showAddTimer by remember { mutableStateOf(false) }
    var showAddReminder by remember { mutableStateOf(false) }
    var alarmsExpanded by remember { mutableStateOf(true) }
    var timersExpanded by remember { mutableStateOf(true) }
    var remindersExpanded by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = { TopAppBar(title = { Text("Schedule") }, colors = crystalTopAppBarColors()) }
    ) { padding ->
        // Stage 15g: same content cap as To-dos — this screen is the other remaining single-column
        // list with no natural detail pane to split against. Stage 16b: sourced from DesktopLayout.
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        LazyColumn(modifier = Modifier.fillMaxHeight().widthIn(max = DesktopLayout.contentMaxWidth).fillMaxWidth()) {
            alarmsSection(
                alarms = alarms,
                expanded = alarmsExpanded,
                onToggleExpanded = { alarmsExpanded = !alarmsExpanded },
                onAdd = { showAddAlarm = true },
                onToggleEnabled = alarmViewModel::toggleEnabled,
                onDelete = alarmViewModel::deleteAlarm
            )
            timersSection(
                timerStates = timerStates,
                expanded = timersExpanded,
                onToggleExpanded = { timersExpanded = !timersExpanded },
                onAdd = { showAddTimer = true },
                onStart = timerViewModel::startTimer,
                onPause = timerViewModel::pauseTimer,
                onReset = timerViewModel::resetTimer,
                onDelete = timerViewModel::deleteTimer
            )
            remindersSection(
                reminders = upcoming + past + completed,
                expanded = remindersExpanded,
                onToggleExpanded = { remindersExpanded = !remindersExpanded },
                onAdd = { showAddReminder = true },
                onToggle = reminderViewModel::toggleCompletion,
                onDelete = reminderViewModel::deleteReminder
            )
        }
        }
    }

    if (showAddAlarm) {
        DesktopAddAlarmSheet(
            onDismiss = { showAddAlarm = false },
            onSave = { label, hour, minute, repeatDaysMask ->
                alarmViewModel.addAlarm(label, hour, minute, repeatDaysMask, soundUri = "")
                showAddAlarm = false
            }
        )
    }

    if (showAddTimer) {
        DesktopAddTimerSheet(
            onDismiss = { showAddTimer = false },
            onSave = { label, durationSeconds ->
                timerViewModel.addTimer(label, durationSeconds)
                showAddTimer = false
            },
            onSavePomo = { label, workMin, shortBreakMin, longBreakMin, sessions ->
                timerViewModel.addPomoTimer(label, workMin * 60, shortBreakMin * 60, longBreakMin * 60, sessions)
                showAddTimer = false
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

private fun LazyListScope.alarmsSection(
    alarms: List<Alarm>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAdd: () -> Unit,
    onToggleEnabled: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit
) {
    item(key = "alarms_header") {
        CollapsibleSectionHeader(title = "Alarms", expanded = expanded, onToggle = onToggleExpanded, tier = SectionHeaderTier.Primary)
    }
    if (!expanded) return

    item(key = "alarms_add") {
        Button(
            onClick = onAdd,
            colors = crystalButtonColors(),
            modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing)
        ) { Text("+ Alarm") }
    }
    if (alarms.isEmpty()) {
        item(key = "alarms_empty") {
            Text("No alarms yet.", modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing))
        }
    } else {
        items(alarms, key = { "alarm_${it.id}" }) { alarm ->
            Box(Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding, vertical = 4.dp).crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(DesktopLayout.cardPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text("%02d:%02d".format(alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (alarm.label.isNotBlank()) alarm.label else if (alarm.repeatDaysMask == 0) "One-time" else "Repeats weekly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = alarm.isEnabled, onCheckedChange = { onToggleEnabled(alarm) }, colors = crystalSwitchColors())
                    TextButton(onClick = { onDelete(alarm) }) { Text("Delete") }
                }
            }
        }
    }
}

private fun LazyListScope.timersSection(
    timerStates: List<TimerUiState>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAdd: () -> Unit,
    onStart: (com.apagon.rhythm.data.model.Timer) -> Unit,
    onPause: (com.apagon.rhythm.data.model.Timer) -> Unit,
    onReset: (com.apagon.rhythm.data.model.Timer) -> Unit,
    onDelete: (com.apagon.rhythm.data.model.Timer) -> Unit
) {
    item(key = "timers_header") {
        CollapsibleSectionHeader(title = "Timers", expanded = expanded, onToggle = onToggleExpanded, tier = SectionHeaderTier.Primary)
    }
    if (!expanded) return

    item(key = "timers_add") {
        Button(
            onClick = onAdd,
            colors = crystalButtonColors(),
            modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing)
        ) { Text("+ Timer") }
    }
    if (timerStates.isEmpty()) {
        item(key = "timers_empty") {
            Text("No timers yet.", modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing))
        }
    } else {
        items(timerStates, key = { "timer_${it.timer.id}" }) { state ->
            val timer = state.timer
            Box(Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding, vertical = 4.dp).crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.fillMaxWidth().padding(DesktopLayout.cardPadding)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                timer.label.ifBlank { "Timer" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
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
                            TextButton(onClick = { onPause(timer) }) { Text("Pause") }
                        } else {
                            TextButton(onClick = { onStart(timer) }) { Text("Start") }
                        }
                        TextButton(onClick = { onReset(timer) }) { Text("Reset") }
                        TextButton(onClick = { onDelete(timer) }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.remindersSection(
    reminders: List<Reminder>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAdd: () -> Unit,
    onToggle: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit
) {
    item(key = "reminders_header") {
        CollapsibleSectionHeader(title = "Reminders", expanded = expanded, onToggle = onToggleExpanded, tier = SectionHeaderTier.Primary)
    }
    if (!expanded) return

    item(key = "reminders_add") {
        Button(
            onClick = onAdd,
            colors = crystalButtonColors(),
            modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing)
        ) { Text("+ Reminder") }
    }
    if (reminders.isEmpty()) {
        item(key = "reminders_empty") {
            Text("No reminders yet.", modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing))
        }
    } else {
        items(reminders, key = { "reminder_${it.id}" }) { reminder ->
            Box(Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = 4.dp)) {
                DesktopReminderRow(reminder, onToggle = { onToggle(reminder) }, onDelete = { onDelete(reminder) })
            }
        }
    }
}

@Composable
private fun DesktopReminderRow(reminder: Reminder, onToggle: () -> Unit, onDelete: () -> Unit) {
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

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
