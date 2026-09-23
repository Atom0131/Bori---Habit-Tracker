package com.apagon.rhythm.ui.alarms

import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.apagon.rhythm.ui.util.HabitCard
import java.text.DateFormat
import java.util.Date

@Composable
fun TimerTab(viewModel: TimerViewModel = koinViewModel()) {
    val states by viewModel.timerUiStates.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (states.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No timers yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(states, key = { it.timer.id }) { state ->
                    TimerRow(
                        state = state,
                        onStart = { viewModel.startTimer(state.timer) },
                        onPause = { viewModel.pauseTimer(state.timer) },
                        onReset = { viewModel.resetTimer(state.timer) },
                        onDelete = { viewModel.deleteTimer(state.timer) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add timer")
        }
    }

    if (showAddSheet) {
        AddTimerSheet(
            onDismiss = { showAddSheet = false },
            onSave = { label, duration, soundUri, vibrationPatternId, isPomo, pomoWorkSecs, pomoShortSecs, pomoLongSecs, pomoSessions ->
                if (isPomo) {
                    viewModel.addPomoTimer(label, pomoWorkSecs, pomoShortSecs, pomoLongSecs, pomoSessions, soundUri, vibrationPatternId)
                } else {
                    viewModel.addTimer(label, duration, soundUri, vibrationPatternId)
                }
                showAddSheet = false
            }
        )
    }
}

@Composable
private fun TimerRow(
    state: TimerUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = state.displayRemaining == 0 && !state.isRunning
    val totalSeconds = state.timer.durationSeconds.coerceAtLeast(1)
    val progress = state.displayRemaining.toFloat() / totalSeconds.toFloat()

    val ringColor = when {
        isDone         -> MaterialTheme.colorScheme.primary
        state.isRunning -> MaterialTheme.colorScheme.primary
        else           -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    HabitCard(isDone = isDone) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular progress ring with remaining time inside
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(84.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 6.dp,
                    color = ringColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = formatDuration(state.displayRemaining),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ringColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Labels and metadata
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (state.timer.label.isNotBlank()) {
                    Text(
                        text = state.timer.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (state.pomoPhaseLabel != null) {
                    Text(
                        text = state.pomoPhaseLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val totalLabel = if (state.timer.isPomo) {
                    "Work: ${formatDuration(state.timer.pomoWorkSecs)}"
                } else {
                    "Total: ${formatDuration(state.timer.durationSeconds)}"
                }
                Text(
                    text = totalLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.isRunning && state.timer.endTimeMillis > 0) {
                    Text(
                        text = "Ends at ${formatEndTime(state.timer.endTimeMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action buttons
            Row {
                if (state.isRunning) {
                    IconButton(onClick = onPause) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Pause",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(
                        onClick = onStart,
                        enabled = state.displayRemaining > 0
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Start",
                            tint = if (state.displayRemaining > 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
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

private fun formatEndTime(endMillis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(endMillis))
