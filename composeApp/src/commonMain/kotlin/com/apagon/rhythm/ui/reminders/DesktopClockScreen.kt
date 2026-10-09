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
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.Icons
import com.apagon.rhythm.ui.components.AddTypePickerSheet
import com.apagon.rhythm.ui.components.AddOption
import org.koin.compose.koinInject
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import com.apagon.rhythm.ui.components.isCrystal
import com.apagon.rhythm.ui.components.crystalIconButtonSurface
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.onClick
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.ExperimentalFoundationApi

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
        topBar = { TopAppBar(title = { Text("Alarms & Timers", fontWeight = FontWeight.Bold) }, colors = crystalTopAppBarColors()) },
        floatingActionButton = {
            RhythmAddFab(onClick = { showAddMenu = true })
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            BoxWithConstraints(modifier = Modifier.fillMaxHeight().widthIn(max = DesktopLayout.contentMaxWidth).fillMaxWidth()) {
                val gridColumns = ((maxWidth - DesktopLayout.screenPadding * 2) / GRID_MIN_CARD_WIDTH)
                    .toInt()
                    .coerceIn(2, 4)
                val localeFormatting = koinInject<LocaleFormatting>()
                val is24Hour = remember(localeFormatting) { localeFormatting.is24HourFormat() }
                // As Android's FocusList: a section appears only once it has something in it, and an
                // empty screen gets one quiet message instead of three "No … yet" lines.
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (alarms.isNotEmpty()) {
                        alarmsSection(
                            alarms = alarms,
                            gridColumns = gridColumns,
                            is24Hour = is24Hour,
                            onToggleEnabled = alarmViewModel::toggleEnabled,
                            onDelete = alarmViewModel::deleteAlarm
                        )
                    }
                    if (regularTimers.isNotEmpty()) {
                        timersSection(
                            title = "Active Timers",
                            timerStates = regularTimers,
                            gridColumns = gridColumns,
                            onStart = timerViewModel::startTimer,
                            onPause = timerViewModel::pauseTimer,
                            onReset = timerViewModel::resetTimer,
                            onDelete = timerViewModel::deleteTimer
                        )
                    }
                    if (pomoTimers.isNotEmpty()) {
                        timersSection(
                            title = "Pomodoro",
                            timerStates = pomoTimers,
                            gridColumns = gridColumns,
                            onStart = timerViewModel::startTimer,
                            onPause = timerViewModel::pauseTimer,
                            onReset = timerViewModel::resetTimer,
                            onDelete = timerViewModel::deleteTimer
                        )
                    }
                    if (alarms.isEmpty() && timerStates.isEmpty()) {
                        item(key = "focus_empty") { EmptyFocusState() }
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }

    if (showAddMenu) {
        AddTypePickerSheet(
            onDismiss = { showAddMenu = false },
            options = listOf(
                AddOption("alarm", "Alarm", Icons.Default.Alarm, "Schedule wake-up alerts"),
                AddOption("timer", "Timer", Icons.Default.Timer, "Countdown for tasks"),
                AddOption("pomo", "Pomodoro", Icons.Default.Coffee, "Focus/Break sessions")
            ),
            onOptionSelected = { id ->
                when (id) {
                    "alarm" -> showAddAlarm = true
                    "timer" -> { addTimerIsPomo = false; showAddTimer = true }
                    "pomo" -> { addTimerIsPomo = true; showAddTimer = true }
                }
            }
        )
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

/** Android uses 170dp, sized for its 24-hour "14:50". The desktop follows the PC's clock format,
 * and a 12-hour "12:45 PM" beside the switch needs about 220dp before it starts to ellipsize. */
private val GRID_MIN_CARD_WIDTH = 220.dp

private val DAY_NAMES_SINGLE = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing)
    )
}

@Composable
private fun EmptyFocusState() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No focus tools set", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        Text("Tap + to add an alarm or timer", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

private fun LazyListScope.alarmsSection(
    alarms: List<Alarm>,
    gridColumns: Int,
    is24Hour: Boolean,
    onToggleEnabled: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit
) {
    item(key = "alarms_header") { SectionHeader("Scheduled Alarms") }
    items(alarms.chunked(gridColumns), key = { it.first().id }) { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            row.forEach { alarm ->
                AlarmCard(alarm, is24Hour, Modifier.weight(1f), onToggleEnabled = { onToggleEnabled(alarm) }, onDelete = { onDelete(alarm) })
            }
            repeat(gridColumns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/**
 * Port of Android's `FluidAlarmCard`: a square glass card, the time with the switch beside it, the
 * label, and the repeat days as a row of small circles at the bottom. Delete is a right-click (a long
 * press on the phone) behind a confirm, as on Android, instead of a text button on every card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(alarm: Alarm, is24Hour: Boolean, modifier: Modifier, onToggleEnabled: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        RhythmAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Alarm?") },
            text = { Text("Are you sure you want to delete this alarm?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
    Box(
        modifier
            .aspectRatio(1f)
            .crystalCardSurface(
                fill = if (alarm.isEnabled) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) { showDeleteConfirm = true }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(alarm.hour, alarm.minute, is24Hour),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    colors = crystalSwitchColors(),
                    modifier = Modifier.scale(0.75f)
                )
            }
            if (alarm.label.isNotBlank()) {
                Text(
                    alarm.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.weight(1f))
            RepeatDaysRow(mask = alarm.repeatDaysMask, enabled = alarm.isEnabled)
        }
    }
}

@Composable
private fun RepeatDaysRow(mask: Int, enabled: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        DAY_NAMES_SINGLE.forEachIndexed { index, day ->
            val active = (mask and (1 shl index)) != 0
            val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
            Box(
                modifier = Modifier.size(16.dp).clip(CircleShape).background(if (enabled) color else color.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    day,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun LazyListScope.timersSection(
    title: String,
    timerStates: List<TimerUiState>,
    gridColumns: Int,
    onStart: (com.apagon.rhythm.data.model.Timer) -> Unit,
    onPause: (com.apagon.rhythm.data.model.Timer) -> Unit,
    onReset: (com.apagon.rhythm.data.model.Timer) -> Unit,
    onDelete: (com.apagon.rhythm.data.model.Timer) -> Unit
) {
    item(key = "${title}_header") { SectionHeader(title) }
    items(timerStates.chunked(gridColumns), key = { "${title}_${it.first().timer.id}" }) { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            row.forEach { state ->
                TimerCard(
                    state, Modifier.weight(1f),
                    onStart = { onStart(state.timer) }, onPause = { onPause(state.timer) },
                    onReset = { onReset(state.timer) }, onDelete = { onDelete(state.timer) }
                )
            }
            repeat(gridColumns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/**
 * Port of Android's `FluidTimerCard`: a square glass card with the remaining time (accent while
 * running, plus a small dot top-right), the label, the Pomodoro phase, and a row of glass discs:
 * Reset, Delete (behind a confirm; timers are hard-deleted) and a larger Play/Pause.
 */
@Composable
private fun TimerCard(state: TimerUiState, modifier: Modifier, onStart: () -> Unit, onPause: () -> Unit, onReset: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        val label = state.timer.label
        RhythmAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Timer?") },
            text = { Text(if (label.isBlank()) "This timer will be deleted." else "\"$label\" will be deleted.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
    Box(modifier.aspectRatio(1f).crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerHighest)) {
        if (state.isRunning) {
            Box(Modifier.align(Alignment.TopEnd).padding(12.dp).size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Text(
                formatDuration(state.displayRemaining),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (state.timer.label.isNotBlank()) {
                Text(state.timer.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            state.pomoPhaseLabel?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val gap = 8.dp
                val secondary = ((maxWidth - gap * 2) / 3.25f).coerceIn(24.dp, 40.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimerGlassButton(Icons.Default.Refresh, "Reset", secondary, onReset)
                    TimerGlassButton(Icons.Default.Delete, "Delete", secondary, onClick = { showDeleteConfirm = true })
                    TimerGlassButton(
                        icon = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        label = if (state.isRunning) "Pause" else "Play",
                        diameter = secondary * 1.25f,
                        accent = true,
                        enabled = state.isRunning || state.displayRemaining > 0,
                        onClick = { if (state.isRunning) onPause() else onStart() }
                    )
                }
            }
        }
    }
}

/** Android's `TimerGlassButton`: a glass disc of exactly [diameter] (a Box, not an M3 icon button,
 * whose 48dp minimum made the discs overlap on the phone). */
@Composable
private fun TimerGlassButton(icon: ImageVector, label: String, diameter: Dp, onClick: () -> Unit, accent: Boolean = false, enabled: Boolean = true) {
    val crystal = isCrystal()
    val container = when {
        crystal -> Color.Transparent
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
        accent -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.outline
        accent -> if (crystal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier.size(diameter).crystalIconButtonSurface().clip(CircleShape).background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(diameter * 0.5f))
    }
}

private fun formatTime(hour: Int, minute: Int, is24Hour: Boolean): String =
    if (is24Hour) "%02d:%02d".format(hour, minute)
    else "%d:%02d %s".format(when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }, minute, if (hour < 12) "AM" else "PM")

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
