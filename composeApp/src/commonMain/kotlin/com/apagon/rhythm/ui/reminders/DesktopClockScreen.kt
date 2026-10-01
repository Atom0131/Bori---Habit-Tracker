package com.apagon.rhythm.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.DropdownMenuItem
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
import com.apagon.rhythm.ui.alarms.AlarmViewModel
import com.apagon.rhythm.ui.alarms.DesktopAddAlarmSheet
import com.apagon.rhythm.ui.alarms.DesktopAddTimerSheet
import com.apagon.rhythm.ui.alarms.TimerUiState
import com.apagon.rhythm.ui.alarms.TimerViewModel
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalSwitchColors
import com.apagon.rhythm.ui.util.RhythmAddFab
import com.apagon.rhythm.ui.util.RhythmDropdownMenu
import org.koin.compose.viewmodel.koinViewModel

// Stage 17d: rebuilt to match the real Android ui/reminders/ClockScreen.kt (titled "Alarms &
// Timers") instead of the earlier collapsible-accordion guess. Mobile has no collapse behavior at
// all — three always-open responsive grids (Scheduled Alarms, Active Timers, Pomodoro, split by
// Timer.isPomo) — and Reminders isn't part of this screen on mobile; it lives on the Today/
// Calendar day-detail screen instead (moved there in Stage 17e). gridColumns computed the same
// way mobile does it (GRID_MIN_CARD_WIDTH/GRID_HORIZONTAL_PADDING, coerced 2-4), chunked rows
// instead of a true LazyVerticalGrid so a Pomodoro-vs-regular split and per-section headers stay
// simple inside one scrollable column.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopClockScreen(
    alarmViewModel: AlarmViewModel = koinViewModel(),
    timerViewModel: TimerViewModel = koinViewModel()
) {
    val alarms by alarmViewModel.alarms.collectAsState()
    val timerStates by timerViewModel.timerUiStates.collectAsState()
    val regularTimers = remember(timerStates) { timerStates.filter { !it.timer.isPomo } }
    val pomoTimers = remember(timerStates) { timerStates.filter { it.timer.isPomo } }

    var showAddAlarm by remember { mutableStateOf(false) }
    var showAddTimer by remember { mutableStateOf(false) }
    var addTimerIsPomo by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = { TopAppBar(title = { Text("Schedule") }, colors = crystalTopAppBarColors()) },
        floatingActionButton = {
            Box {
                RhythmAddFab(onClick = { showAddMenu = true })
                RhythmDropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Alarm") },
                        onClick = { showAddMenu = false; showAddAlarm = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Timer") },
                        onClick = { showAddMenu = false; addTimerIsPomo = false; showAddTimer = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Pomodoro") },
                        onClick = { showAddMenu = false; addTimerIsPomo = true; showAddTimer = true }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            BoxWithConstraints(modifier = Modifier.fillMaxHeight().widthIn(max = DesktopLayout.contentMaxWidth).fillMaxWidth()) {
                val gridColumns = ((maxWidth - DesktopLayout.screenPadding * 2) / GRID_MIN_CARD_WIDTH)
                    .toInt()
                    .coerceIn(2, 4)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    alarmsSection(
                        alarms = alarms,
                        gridColumns = gridColumns,
                        onToggleEnabled = alarmViewModel::toggleEnabled,
                        onDelete = alarmViewModel::deleteAlarm
                    )
                    timersSection(
                        title = "Active Timers",
                        emptyLabel = "No timers yet.",
                        timerStates = regularTimers,
                        gridColumns = gridColumns,
                        onStart = timerViewModel::startTimer,
                        onPause = timerViewModel::pauseTimer,
                        onReset = timerViewModel::resetTimer,
                        onDelete = timerViewModel::deleteTimer
                    )
                    if (pomoTimers.isNotEmpty()) {
                        timersSection(
                            title = "Pomodoro",
                            emptyLabel = "No Pomodoro timers yet.",
                            timerStates = pomoTimers,
                            gridColumns = gridColumns,
                            onStart = timerViewModel::startTimer,
                            onPause = timerViewModel::pauseTimer,
                            onReset = timerViewModel::resetTimer,
                            onDelete = timerViewModel::deleteTimer
                        )
                    }
                }
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
            initialPomo = addTimerIsPomo,
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
}

// Wider than mobile's 170dp: mobile's cards use compact icon buttons (no icon library on
// desktop), so desktop's text-labelled "Pause/Start  Reset  Delete" row needs more room to avoid
// clipping the last button.
private val GRID_MIN_CARD_WIDTH = 260.dp

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing)
    )
}

private fun LazyListScope.alarmsSection(
    alarms: List<Alarm>,
    gridColumns: Int,
    onToggleEnabled: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit
) {
    item(key = "alarms_header") { SectionHeader("Scheduled Alarms") }

    if (alarms.isEmpty()) {
        item(key = "alarms_empty") {
            Text("No alarms yet.", modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing))
        }
    } else {
        items(alarms.chunked(gridColumns), key = { it.first().id }) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(DesktopLayout.itemSpacing)
            ) {
                row.forEach { alarm ->
                    Box(modifier = Modifier.weight(1f)) {
                        AlarmCard(alarm, onToggleEnabled = { onToggleEnabled(alarm) }, onDelete = { onDelete(alarm) })
                    }
                }
                repeat(gridColumns - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onToggleEnabled: () -> Unit, onDelete: () -> Unit) {
    Box(Modifier.fillMaxWidth().crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesktopLayout.cardPadding)) {
            Text("%02d:%02d".format(alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (alarm.label.isNotBlank()) alarm.label else if (alarm.repeatDaysMask == 0) "One-time" else "Repeats weekly",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = DesktopLayout.itemSpacing),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked = alarm.isEnabled, onCheckedChange = { onToggleEnabled() }, colors = crystalSwitchColors())
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

private fun LazyListScope.timersSection(
    title: String,
    emptyLabel: String,
    timerStates: List<TimerUiState>,
    gridColumns: Int,
    onStart: (com.apagon.rhythm.data.model.Timer) -> Unit,
    onPause: (com.apagon.rhythm.data.model.Timer) -> Unit,
    onReset: (com.apagon.rhythm.data.model.Timer) -> Unit,
    onDelete: (com.apagon.rhythm.data.model.Timer) -> Unit
) {
    item(key = "${title}_header") { SectionHeader(title) }

    if (timerStates.isEmpty()) {
        item(key = "${title}_empty") {
            Text(emptyLabel, modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing))
        }
    } else {
        items(timerStates.chunked(gridColumns), key = { "${title}_${it.first().timer.id}" }) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(DesktopLayout.itemSpacing)
            ) {
                row.forEach { state ->
                    Box(modifier = Modifier.weight(1f)) {
                        TimerCard(state, onStart = { onStart(state.timer) }, onPause = { onPause(state.timer) }, onReset = { onReset(state.timer) }, onDelete = { onDelete(state.timer) })
                    }
                }
                repeat(gridColumns - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TimerCard(state: TimerUiState, onStart: () -> Unit, onPause: () -> Unit, onReset: () -> Unit, onDelete: () -> Unit) {
    val timer = state.timer
    Box(Modifier.fillMaxWidth().crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesktopLayout.cardPadding)) {
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
            Text(formatDuration(state.displayRemaining), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (timer.durationSeconds > 0) {
                LinearProgressIndicator(
                    progress = { 1f - (state.displayRemaining.toFloat() / timer.durationSeconds.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isRunning) {
                    TextButton(onClick = onPause) { Text("Pause") }
                } else {
                    TextButton(onClick = onStart) { Text("Start") }
                }
                TextButton(onClick = onReset) { Text("Reset") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
