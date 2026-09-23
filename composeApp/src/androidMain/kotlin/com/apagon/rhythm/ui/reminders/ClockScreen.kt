package com.apagon.rhythm.ui.reminders

import org.koin.compose.viewmodel.koinViewModel

import android.app.NotificationManager
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.ui.alarms.AddAlarmSheet
import com.apagon.rhythm.ui.alarms.AddTimerSheet
import com.apagon.rhythm.ui.alarms.AlarmViewModel
import com.apagon.rhythm.ui.alarms.TimerViewModel
import com.apagon.rhythm.ui.components.AddOption
import com.apagon.rhythm.ui.components.AddTypePickerSheet
import com.apagon.rhythm.ui.util.*
import com.apagon.rhythm.ui.util.findActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

@Composable
fun ClockScreen(
    alarmViewModel: AlarmViewModel = koinViewModel(),
    timerViewModel: TimerViewModel = koinViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val alarms by alarmViewModel.alarms.collectAsState()
    val timerStates by timerViewModel.timerUiStates.collectAsState()
    val isPro by alarmViewModel.isPro.collectAsState()

    var showAddAlarmSheet by remember { mutableStateOf(false) }
    var showAddTimerSheet by remember { mutableStateOf(false) }
    var showAddPomoSheet by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var paywallReason by remember { mutableStateOf<String?>(null) }

    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var editingTimer by remember { mutableStateOf<Timer?>(null) }
    var viewingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var viewingTimerId by remember { mutableStateOf<Long?>(null) }
    var showAddTypePicker by remember { mutableStateOf(false) }

    val fsiContext = LocalContext.current
    var canUseFullScreenIntent by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                fsiContext.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            } else true
        )
    }
    // Re-check on resume so the warning disappears as soon as the user grants it in Settings and comes back.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {
                canUseFullScreenIntent = fsiContext.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Static Editorial Title
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Schedule",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Alarms & Timers",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Full-screen-intent permission warning (Android 14+) — without this permission
            // alarms silently vibrate without waking the screen or showing the alarm UI.
            if (!canUseFullScreenIntent) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
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
                                data = Uri.parse("package:${fsiContext.packageName}")
                            }
                            PermissionUtils.safeStartActivity(fsiContext, intent)
                        }) {
                            Text("Open Settings", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Combined Alarms & Timers Content
            FocusList(
                alarms = alarms,
                timerStates = timerStates,
                onTapAlarm = { viewingAlarm = it },
                onTapTimer = { viewingTimerId = it.timer.id },
                onToggleAlarm = { alarmViewModel.toggleEnabled(it) },
                onStartTimer = { timerViewModel.startTimer(it.timer) },
                onPauseTimer = { timerViewModel.pauseTimer(it.timer) },
                onResetTimer = { timerViewModel.resetTimer(it.timer) },
                onDeleteTimer = { timerViewModel.deleteTimer(it.timer) },
                onDeleteAlarm = { alarmViewModel.deleteAlarm(it) }
            )
        }

        // Universal Add FAB
        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            onClick = { showAddTypePicker = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Item")
        }
    }

    // Picker Sheet
    if (showAddTypePicker) {
        AddTypePickerSheet(
            onDismiss = { showAddTypePicker = false },
            options = listOf(
                AddOption("alarm", "Alarm", Icons.Default.Alarm, "Schedule wake-up alerts"),
                AddOption("timer", "Timer", Icons.Default.Timer, "Countdown for tasks"),
                AddOption("pomo", "Pomodoro", Icons.Default.Coffee, "Focus/Break sessions")
            ),
            onOptionSelected = { id ->
                when (id) {
                    "alarm" -> {
                        if (!isPro && alarms.size >= 3) {
                            paywallReason = "Upgrade to Pro to add more than 3 alarms!"
                            showPaywall = true
                        } else {
                            showAddAlarmSheet = true
                        }
                    }
                    "timer" -> showAddTimerSheet = true
                    "pomo" -> {
                        if (!isPro) {
                            paywallReason = "Exclusive Pomodoro Focus Timers are a Pro feature!"
                            showPaywall = true
                        } else {
                            showAddPomoSheet = true
                        }
                    }
                }
            }
        )
    }

    // Sheets
    if (showAddAlarmSheet) {
        AddAlarmSheet(
            onDismiss = { showAddAlarmSheet = false },
            onSave = { label, hour, minute, repeatDays, soundUri, vibrationPatternId ->
                alarmViewModel.addAlarm(label, hour, minute, repeatDays, soundUri, vibrationPatternId)
                showAddAlarmSheet = false
            }
        )
    }

    viewingAlarm?.let { alarm ->
        AlarmViewSheet(
            alarm = alarm,
            onDismiss = { viewingAlarm = null },
            onEdit = { viewingAlarm = null; editingAlarm = alarm },
            onToggle = { alarmViewModel.toggleEnabled(alarm) },
            onDelete = { alarmViewModel.deleteAlarm(alarm); viewingAlarm = null }
        )
    }

    viewingTimerId?.let { timerId ->
        // Look up live state on every recomposition so the countdown updates.
        val liveState = timerStates.firstOrNull { it.timer.id == timerId }
        if (liveState != null) {
            TimerViewSheet(
                state = liveState,
                onDismiss = { viewingTimerId = null },
                onEdit = { viewingTimerId = null; editingTimer = liveState.timer },
                onStart = { timerViewModel.startTimer(liveState.timer) },
                onPause = { timerViewModel.pauseTimer(liveState.timer) },
                onReset = { timerViewModel.resetTimer(liveState.timer) },
                onDelete = { timerViewModel.deleteTimer(liveState.timer); viewingTimerId = null }
            )
        } else {
            // Timer was deleted while sheet was open
            viewingTimerId = null
        }
    }

    editingAlarm?.let { alarm ->
        AddAlarmSheet(
            existing = alarm,
            onDismiss = { editingAlarm = null },
            onSave = { label, hour, minute, repeatDays, soundUri, vibrationPatternId ->
                alarmViewModel.updateAlarm(alarm.copy(label = label, hour = hour, minute = minute, repeatDaysMask = repeatDays, soundUri = soundUri, vibrationPatternId = vibrationPatternId))
                editingAlarm = null
            }
        )
    }

    editingTimer?.let { timer ->
        AddTimerSheet(
            existing = timer,
            isPro = isPro,
            onDismiss = { editingTimer = null },
            onShowPaywall = {
                paywallReason = it
                showPaywall = true
            },
            onSave = { label, duration, soundUri, vibrationPatternId, isPomo, pomoWork, pomoShort, pomoLong, pomoSessions ->
                if (isPomo) timerViewModel.updatePomoTimer(timer.id, label, pomoWork, pomoShort, pomoLong, pomoSessions, soundUri, vibrationPatternId)
                else timerViewModel.updateTimer(timer.id, label, duration, soundUri, vibrationPatternId)
                editingTimer = null
            }
        )
    }

    if (showAddTimerSheet) {
        AddTimerSheet(
            isPro = isPro,
            onDismiss = { showAddTimerSheet = false },
            onShowPaywall = {
                paywallReason = it
                showPaywall = true
            },
            onSave = { label, duration, soundUri, vibrationPatternId, isPomo, pomoWork, pomoShort, pomoLong, pomoSessions ->
                if (isPomo) timerViewModel.addPomoTimer(label, pomoWork, pomoShort, pomoLong, pomoSessions, soundUri, vibrationPatternId)
                else timerViewModel.addTimer(label, duration, soundUri, vibrationPatternId)
                showAddTimerSheet = false
            }
        )
    }

    if (showAddPomoSheet) {
        AddTimerSheet(
            initialIsPomo = true,
            isPro = isPro,
            onDismiss = { showAddPomoSheet = false },
            onShowPaywall = {
                paywallReason = it
                showPaywall = true
            },
            onSave = { label, _, soundUri, vibrationPatternId, _, pomoWork, pomoShort, pomoLong, pomoSessions ->
                timerViewModel.addPomoTimer(label, pomoWork, pomoShort, pomoLong, pomoSessions, soundUri, vibrationPatternId)
                showAddPomoSheet = false
            }
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
                    alarmViewModel.startBillingFlow(productId)
                }
                showPaywall = false
                paywallReason = null
            }
        )
    }
}

@Composable
private fun FocusList(
    alarms: List<Alarm>,
    timerStates: List<com.apagon.rhythm.ui.alarms.TimerUiState>,
    onTapAlarm: (Alarm) -> Unit,
    onTapTimer: (com.apagon.rhythm.ui.alarms.TimerUiState) -> Unit,
    onToggleAlarm: (Alarm) -> Unit,
    onStartTimer: (com.apagon.rhythm.ui.alarms.TimerUiState) -> Unit,
    onPauseTimer: (com.apagon.rhythm.ui.alarms.TimerUiState) -> Unit,
    onResetTimer: (com.apagon.rhythm.ui.alarms.TimerUiState) -> Unit,
    onDeleteTimer: (com.apagon.rhythm.ui.alarms.TimerUiState) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val regularTimers = remember(timerStates) { timerStates.filter { !it.timer.isPomo } }
    val pomoTimers = remember(timerStates) { timerStates.filter { it.timer.isPomo } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Scheduled Alarms — 2-column square grid
        if (alarms.isNotEmpty()) {
            item {
                Text(
                    text = "Scheduled Alarms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            val alarmChunks = alarms.chunked(2)
            items(alarmChunks, key = { "alarm_row_${it.first().id}" }) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { alarm ->
                        FluidAlarmCard(
                            alarm = alarm,
                            is24Hour = is24Hour,
                            modifier = Modifier.weight(1f),
                            onTap = { onTapAlarm(alarm) },
                            onToggle = { onToggleAlarm(alarm) },
                            onDelete = { onDeleteAlarm(alarm) }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // Active Timers — 2-column square grid (non-Pomodoro only)
        if (regularTimers.isNotEmpty()) {
            item {
                Text(
                    text = "Active Timers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            val chunks = regularTimers.chunked(2)
            items(chunks, key = { "timer_row_${it.first().timer.id}" }) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { state ->
                        FluidTimerCard(
                            state = state,
                            modifier = Modifier.weight(1f),
                            onTap = { onTapTimer(state) },
                            onStart = { onStartTimer(state) },
                            onPause = { onPauseTimer(state) },
                            onReset = { onResetTimer(state) },
                            onDelete = { onDeleteTimer(state) }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // Pomodoro — separate 2-column square grid
        if (pomoTimers.isNotEmpty()) {
            item {
                Text(
                    text = "Pomodoro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            val pomoChunks = pomoTimers.chunked(2)
            items(pomoChunks, key = { "pomo_row_${it.first().timer.id}" }) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { state ->
                        FluidTimerCard(
                            state = state,
                            modifier = Modifier.weight(1f),
                            onTap = { onTapTimer(state) },
                            onStart = { onStartTimer(state) },
                            onPause = { onPauseTimer(state) },
                            onReset = { onResetTimer(state) },
                            onDelete = { onDeleteTimer(state) }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        if (alarms.isEmpty() && timerStates.isEmpty()) {
            item { EmptyFocusState() }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FluidAlarmCard(
    alarm: Alarm,
    is24Hour: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Alarm?") },
            text = { Text("Are you sure you want to delete this alarm?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (alarm.isEnabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onTap,
                onLongClick = { showDeleteConfirm = true }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(alarm.hour, alarm.minute, is24Hour),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.scale(0.75f)
                )
            }
            if (alarm.label.isNotBlank()) {
                Text(
                    text = alarm.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.weight(1f))
            RepeatDaysRow(mask = alarm.repeatDaysMask, enabled = alarm.isEnabled, compact = true)
        }
    }
}

@Composable
private fun RepeatDaysRow(mask: Int, enabled: Boolean, compact: Boolean = false) {
    val circleSize = if (compact) 16.dp else 24.dp
    val spacing = if (compact) 2.dp else 4.dp
    val textStyle = if (compact) MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp) else MaterialTheme.typography.labelSmall
    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
        DAY_NAMES_SINGLE.forEachIndexed { index, day ->
            val active = (mask and (1 shl index)) != 0
            val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
            val tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            Box(
                modifier = Modifier.size(circleSize).clip(CircleShape).background(if (enabled) color else color.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = day, style = textStyle, fontWeight = FontWeight.Bold, color = tint)
            }
        }
    }
}

@Composable
private fun FluidTimerCard(
    state: com.apagon.rhythm.ui.alarms.TimerUiState,
    modifier: Modifier,
    onTap: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onTap,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isRunning) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(7.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Text(
                    text = formatDuration(state.displayRemaining),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isRunning) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                )
                if (state.timer.label.isNotBlank()) {
                    Text(
                        text = state.timer.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (state.pomoPhaseLabel != null) {
                    Text(
                        text = state.pomoPhaseLabel!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    FilledIconButton(
                        onClick = { if (state.isRunning) onPause() else onStart() },
                        modifier = Modifier.size(40.dp),
                        enabled = state.isRunning || state.displayRemaining > 0
                    ) {
                        Icon(
                            imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isRunning) "Pause" else "Play",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600; val m = (totalSeconds % 3600) / 60; val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmViewSheet(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .blockSheetBodyDrag(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = formatTime(alarm.hour, alarm.minute, is24Hour),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (alarm.label.isNotBlank()) {
                Text(
                    text = alarm.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (alarm.repeatDaysMask == 0) {
                Text("One-time alarm", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                RepeatDaysRow(mask = alarm.repeatDaysMask, enabled = alarm.isEnabled, compact = false)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (alarm.isEnabled) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = alarm.isEnabled, onCheckedChange = { onToggle() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerViewSheet(
    state: com.apagon.rhythm.ui.alarms.TimerUiState,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalSeconds = state.timer.durationSeconds.coerceAtLeast(1)
    val progress = state.displayRemaining.toFloat() / totalSeconds.toFloat()
    val ringColor = if (state.isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .blockSheetBodyDrag(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
                if (state.timer.label.isNotBlank()) {
                    Text(
                        text = state.timer.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    color = ringColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatDuration(state.displayRemaining),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = ringColor
                    )
                    if (state.pomoPhaseLabel != null) {
                        Text(
                            text = state.pomoPhaseLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                text = "Total: ${formatDuration(state.timer.durationSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Sound info row
            val context = LocalContext.current
            val soundLabel = remember(state.timer.soundUri) {
                when {
                    state.timer.soundUri.isEmpty() -> "Default alarm"
                    state.timer.soundUri == "silent" -> "Silent"
                    else -> try {
                        RingtoneManager.getRingtone(context, Uri.parse(state.timer.soundUri))
                            ?.getTitle(context) ?: "Custom"
                    } catch (e: Exception) { "Custom" }
                }
            }
            val isSilent = state.timer.soundUri == "silent"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isSilent) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = soundLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(28.dp))
                }
                FilledIconButton(
                    onClick = { if (state.isRunning) onPause() else onStart() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isRunning) "Pause" else "Start",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFocusState() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No focus tools set", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        Text("Tap + to add an alarm or timer", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
