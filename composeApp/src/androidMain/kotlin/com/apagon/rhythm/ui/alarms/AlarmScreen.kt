package com.apagon.rhythm.ui.alarms

import org.koin.compose.viewmodel.koinViewModel

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.FilledIconButton
import com.apagon.rhythm.ui.util.MomentumButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.ui.util.DAY_NAMES_SINGLE
import com.apagon.rhythm.ui.util.HabitCard
import com.apagon.rhythm.ui.util.EditorialTitle
import com.apagon.rhythm.ui.util.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel = koinViewModel(),
    timerViewModel: TimerViewModel = koinViewModel(),
    swipeNavigationEnabled: Boolean = false
) {
    val alarms by viewModel.alarms.collectAsState()
    val timerStates by timerViewModel.timerUiStates.collectAsState()
    var showAddAlarmSheet by remember { mutableStateOf(false) }
    var showAddTimerSheet by remember { mutableStateOf(false) }
    var showAddPomoSheet by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn() + expandVertically(expandFrom = androidx.compose.ui.Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = androidx.compose.ui.Alignment.Bottom)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Pomodoro", style = MaterialTheme.typography.labelLarge)
                            SmallFloatingActionButton(
                                onClick = { fabExpanded = false; showAddPomoSheet = true },
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Coffee, contentDescription = "Add pomodoro")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Add Timer", style = MaterialTheme.typography.labelLarge)
                            SmallFloatingActionButton(
                                onClick = { fabExpanded = false; showAddTimerSheet = true },
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = "Add timer")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Add Alarm", style = MaterialTheme.typography.labelLarge)
                            SmallFloatingActionButton(
                                onClick = { fabExpanded = false; showAddAlarmSheet = true },
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = "Add alarm")
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                EditorialTitle(
                    text = "Alarms and Timers",
                    style = MaterialTheme.typography.displaySmall,
                    padding = 16.dp
                )
            }

            // Permission warning (Android 14+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                item {
                    val context = LocalContext.current
                    val nm = context.getSystemService(NotificationManager::class.java)
                    if (!nm.canUseFullScreenIntent()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Alarms won't wake the screen",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Grant full-screen intent permission so alarms can show on the lock screen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                TextButton(onClick = {
                                    val intent = Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT").apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    com.apagon.rhythm.ui.util.PermissionUtils.safeStartActivity(context, intent)
                                }) {
                                    Text("Open Settings", color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }
            }

            // Alarms section header
            item {
                Text(
                    text = "Alarms",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Alarm rows
            itemsIndexed(alarms, key = { _, alarm -> "alarm_${alarm.id}" }) { _, alarm ->
                HabitCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    AlarmRow(
                        alarm = alarm,
                        onTap = { editingAlarm = alarm },
                        onToggle = { viewModel.toggleEnabled(alarm) },
                        onDelete = { viewModel.deleteAlarm(alarm) }
                    )
                }
            }

            if (alarms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No alarms yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Timers section header
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Timers",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Timer grid (2 columns via chunked rows)
            val timerChunks = timerStates.chunked(2)
            items(timerChunks, key = { chunk -> "timers_${chunk.first().timer.id}" }) { chunk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(androidx.compose.foundation.layout.IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chunk.forEach { state ->
                        TimerGridCard(
                            state = state,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onStart = { timerViewModel.startTimer(state.timer) },
                            onPause = { timerViewModel.pauseTimer(state.timer) },
                            onReset = { timerViewModel.resetTimer(state.timer) },
                            onDelete = { timerViewModel.deleteTimer(state.timer) }
                        )
                    }
                    // Fill empty slot if odd number
                    if (chunk.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (timerStates.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No timers yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddAlarmSheet) {
        AddAlarmSheet(
            onDismiss = { showAddAlarmSheet = false },
            onSave = { label, hour, minute, repeatDays, soundUri, vibrationPatternId ->
                viewModel.addAlarm(label, hour, minute, repeatDays, soundUri, vibrationPatternId)
                showAddAlarmSheet = false
            }
        )
    }

    editingAlarm?.let { alarm ->
        AddAlarmSheet(
            existing = alarm,
            onDismiss = { editingAlarm = null },
            onSave = { label, hour, minute, repeatDays, soundUri, vibrationPatternId ->
                viewModel.updateAlarm(
                    alarm.copy(label = label, hour = hour, minute = minute, repeatDaysMask = repeatDays, soundUri = soundUri, vibrationPatternId = vibrationPatternId)
                )
                editingAlarm = null
            }
        )
    }

    if (showAddPomoSheet) {
        AddTimerSheet(
            initialIsPomo = true,
            onDismiss = { showAddPomoSheet = false },
            onSave = { label, _, soundUri, vibrationPatternId, _, pomoWorkSecs, pomoShortSecs, pomoLongSecs, pomoSessions ->
                timerViewModel.addPomoTimer(label, pomoWorkSecs, pomoShortSecs, pomoLongSecs, pomoSessions, soundUri, vibrationPatternId)
                showAddPomoSheet = false
            }
        )
    }

    if (showAddTimerSheet) {
        AddTimerSheet(
            onDismiss = { showAddTimerSheet = false },
            onSave = { label, duration, soundUri, vibrationPatternId, isPomo, pomoWorkSecs, pomoShortSecs, pomoLongSecs, pomoSessions ->
                if (isPomo) {
                    timerViewModel.addPomoTimer(label, pomoWorkSecs, pomoShortSecs, pomoLongSecs, pomoSessions, soundUri, vibrationPatternId)
                } else {
                    timerViewModel.addTimer(label, duration, soundUri, vibrationPatternId)
                }
                showAddTimerSheet = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmRow(
    alarm: Alarm, 
    onTap: () -> Unit, 
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val primary = MaterialTheme.colorScheme.primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatTime(alarm.hour, alarm.minute, is24Hour),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (alarm.isEnabled) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (alarm.label.isNotBlank()) {
                Text(
                    text = alarm.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            if (alarm.repeatDaysMask == 0) {
                Text(
                    text = "Once",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    DAY_NAMES_SINGLE.forEachIndexed { index, dayLabel ->
                        val active = (alarm.repeatDaysMask and (1 shl index)) != 0
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) primary
                                    else primary.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else primary
                            )
                        }
                    }
                }
            }
        }
        
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete Alarm",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Switch(
            checked = alarm.isEnabled,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun TimerGridCard(
    state: TimerUiState,
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = state.displayRemaining == 0 && !state.isRunning

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDuration(state.displayRemaining),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                    )
                    if (state.timer.label.isNotBlank()) {
                        Text(
                            text = state.timer.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.pomoPhaseLabel != null) {
                        Text(
                            text = state.pomoPhaseLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            MomentumButton(
                text = if (state.isRunning) "Pause" else "Play",
                onClick = { if (state.isRunning) onPause() else onStart() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isRunning || state.displayRemaining > 0
            )
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
