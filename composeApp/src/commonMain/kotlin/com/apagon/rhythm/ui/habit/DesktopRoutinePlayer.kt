package com.apagon.rhythm.ui.habit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.RoutineRunLog
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.routine.RoutineRun
import com.apagon.rhythm.data.routine.RoutineRunner
import com.apagon.rhythm.data.routine.RoutineStepStatus
import com.apagon.rhythm.data.routine.formatCountdown
import com.apagon.rhythm.data.routine.formatStepDuration
import com.apagon.rhythm.data.routine.routineTotalSeconds
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalIconButtonSurface
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.util.MomentumButton
import com.apagon.rhythm.ui.util.formatClockTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.time.Instant

/**
 * Port of the phone's `RoutinePlayerScreen` (Android 1.88), shown in Today's right-hand pane. Three
 * faces, chosen by the runner's state rather than anything held here: **ready** (no run for this
 * habit: the steps and Start), **running** (the current step's countdown and controls), and
 * **summary** (the run has left its last step).
 *
 * Leaving the pane doesn't stop the run: the sidebar card is the way back in. Only "End routine"
 * (or closing the summary) clears it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopRoutinePlayer(habitId: Long, onBack: () -> Unit) {
    val runner = koinInject<RoutineRunner>()
    val habits = koinInject<HabitRepository>()
    val prefs = koinInject<ThemePreferences>()
    val is24Hour = koinInject<LocaleFormatting>().let { remember(it) { it.is24HourFormat() } }
    val scope = rememberCoroutineScope()

    val habit by remember(habitId) { habits.getHabitById(habitId) }.collectAsState(initial = null)
    val items by remember(habitId) { habits.getItemsForHabit(habitId) }.collectAsState(initial = emptyList())
    val history by remember(habitId) { runner.recentRuns(habitId) }.collectAsState(initial = emptyList())
    val current by runner.run.collectAsState()
    val speak by prefs.routineSpeakSteps.collectAsState(initial = false)
    val run = current?.takeIf { it.habitId == habitId }
    val otherRun = current?.takeIf { it.habitId != habitId && !it.isFinished }
    val steps = items.sortedBy { it.sortOrder }

    // A slow clock for the text that only needs the minute ("done by 7:42"). The ring and digits
    // run on the frame clock inside [RunningHeader].
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        runner.clearStaleSummary()
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    // The runner advances the step by itself; this just makes the change land on zero.
    LaunchedEffect(run?.currentIndex, run?.stepEndMillis, run?.isPaused) {
        val r = run ?: return@LaunchedEffect
        if (r.isPaused || r.isFinished || r.stepEndMillis <= 0) return@LaunchedEffect
        delay((r.stepEndMillis - System.currentTimeMillis()).coerceAtLeast(0))
        runner.onTimeUp()
    }
    fun clock(millis: Long): String {
        val t = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
        return formatClockTime(t.hour, t.minute, is24Hour)
    }

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = {
            TopAppBar(
                title = { Text(habit?.name ?: run?.habitName ?: "Routine") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    // Spoken steps: one switch for every routine, kept with the routine itself.
                    IconButton(onClick = { scope.launch { prefs.setRoutineSpeakSteps(!speak) } }) {
                        Icon(
                            if (speak) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (speak) "Stop reading steps aloud" else "Read steps aloud",
                            tint = if (speak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (run != null && !run.isFinished) {
                        TextButton(onClick = { scope.launch { runner.end() }; onBack() }) { Text("End routine") }
                    }
                },
                colors = crystalTopAppBarColors()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (speak) runner.missingVoiceHint?.let { hint ->
                item(key = "voice_hint") {
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when {
                run == null -> {
                    item {
                        ReadyHeader(
                            totalSeconds = steps.routineTotalSeconds(),
                            stepCount = steps.size,
                            finishBy = clock(System.currentTimeMillis() + steps.routineTotalSeconds() * 1000L),
                            otherRoutineName = otherRun?.habitName,
                            onStart = { scope.launch { runner.start(habitId) } }
                        )
                    }
                    itemsIndexed(steps, key = { _, it -> it.id }) { _, item ->
                        StepRow(item.label, item.durationSeconds, StepMark.Pending)
                    }
                    if (history.isNotEmpty()) {
                        item(key = "history") { RunHistory(history, ::clock) }
                    }
                }
                run.isFinished -> {
                    item {
                        SummaryHeader(
                            run = run,
                            onMarkAllDone = { scope.launch { runner.markAllDone() } },
                            onClose = { scope.launch { runner.end() }; onBack() }
                        )
                    }
                    itemsIndexed(run.steps, key = { i, it -> "${it.itemId}_$i" }) { _, step ->
                        StepRow(step.label, step.durationSeconds, step.status.toMark(current = false))
                    }
                }
                else -> {
                    item {
                        RunningHeader(
                            run = run,
                            finishBy = clock(now + run.remainingTotalMs(now)),
                            onPause = { scope.launch { runner.pause() } },
                            onResume = { scope.launch { runner.resume() } },
                            onAddMinute = { scope.launch { runner.addMinute() } },
                            onSkip = { scope.launch { runner.skipCurrent() } },
                            onDone = { scope.launch { runner.completeCurrent() } }
                        )
                    }
                    itemsIndexed(run.steps, key = { i, it -> "${it.itemId}_$i" }) { index, step ->
                        StepRow(step.label, step.durationSeconds, step.status.toMark(current = index == run.currentIndex))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyHeader(totalSeconds: Int, stepCount: Int, finishBy: String, otherRoutineName: String?, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().crystalCardSurface().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (totalSeconds > 0) formatStepDuration(totalSeconds) else "$stepCount steps",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        if (totalSeconds > 0) {
            Text(
                "$stepCount steps · done by $finishBy if you start now",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (otherRoutineName != null) {
            Text(
                "\"$otherRoutineName\" is running. Starting this one will end it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(4.dp))
        MomentumButton(text = "Start routine", onClick = onStart, enabled = stepCount > 0, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RunningHeader(
    run: RoutineRun,
    finishBy: String,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAddMinute: () -> Unit,
    onSkip: () -> Unit,
    onDone: () -> Unit
) {
    val step = run.currentStep ?: return
    val timed = step.durationSeconds > 0

    // The countdown runs on the frame clock, and the ring reads it only while drawing, so a frame
    // costs a redraw of the ring and nothing else. It stops while paused or on an untimed step.
    val frameNow = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(run.currentIndex, run.stepEndMillis, run.isPaused) {
        frameNow.longValue = System.currentTimeMillis()
        if (run.isPaused || run.stepEndMillis <= 0) return@LaunchedEffect
        while (true) {
            withFrameMillis { frameNow.longValue = System.currentTimeMillis() }
        }
    }
    val currentRun by rememberUpdatedState(run)
    // Whole seconds left, rounded up: the digits recompose exactly when they tick.
    val secondsLeft by remember {
        derivedStateOf { ((currentRun.remainingMs(frameNow.longValue) ?: 0L) + 999) / 1000 }
    }
    // A new step refills the ring from empty over 300 ms instead of snapping back to full.
    val refill = remember(run.currentIndex) { Animatable(0f) }
    LaunchedEffect(run.currentIndex) { refill.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
    val nextStep = run.steps.drop(run.currentIndex + 1).firstOrNull { it.status == RoutineStepStatus.PENDING }

    Column(
        modifier = Modifier.fillMaxWidth().crystalCardSurface().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Step ${run.currentIndex + 1} of ${run.steps.size} · done by $finishBy",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            if (timed) {
                CircularProgressIndicator(
                    progress = {
                        val remaining = currentRun.remainingMs(frameNow.longValue) ?: 0L
                        (remaining / (step.durationSeconds * 1000f)).coerceIn(0f, 1f) * refill.value
                    },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                Text(
                    step.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (timed) formatCountdown(secondsLeft) else "No timer",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (run.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
                if (run.isPaused) {
                    Text("Paused", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (timed) {
                RoundControl(
                    if (run.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    if (run.isPaused) "Resume" else "Pause",
                    if (run.isPaused) onResume else onPause
                )
                TextButton(onClick = onAddMinute) { Text("+1 min") }
            }
            RoundControl(Icons.Default.SkipNext, "Skip", onSkip)
            RoundControl(Icons.Default.Check, "Done", onDone)
        }

        Text(
            nextStep?.let { "Next: ${it.label}" + if (it.durationSeconds > 0) " (${formatStepDuration(it.durationSeconds)})" else "" }
                ?: "Last step",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RoundControl(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(56.dp).crystalIconButtonSurface()) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SummaryHeader(run: RoutineRun, onMarkAllDone: () -> Unit, onClose: () -> Unit) {
    val done = run.steps.count { it.status == RoutineStepStatus.DONE }
    val skipped = run.steps.size - done
    val plannedSeconds = run.steps.sumOf { it.durationSeconds }
    val actualSeconds = ((run.finishedAt - run.startedAt) / 1000).toInt().coerceAtLeast(0)
    Column(
        modifier = Modifier.fillMaxWidth().crystalCardSurface().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (skipped == 0) "Routine complete" else "Routine finished",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "$done of ${run.steps.size} steps done" + if (skipped > 0) " · $skipped skipped" else "",
            style = MaterialTheme.typography.bodyMedium
        )
        if (plannedSeconds > 0) {
            Text(
                "Took ${formatStepDuration(actualSeconds)} · planned ${formatStepDuration(plannedSeconds)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        if (skipped > 0) {
            // A skipped step leaves the habit unticked, by design: this is the deliberate way past it.
            TextButton(onClick = onMarkAllDone) { Text("Mark routine done anyway") }
        }
        MomentumButton(text = "Close", onClick = onClose, modifier = Modifier.fillMaxWidth())
    }
}

private enum class StepMark { Pending, Current, Done, Skipped }

private fun RoutineStepStatus.toMark(current: Boolean): StepMark = when {
    current -> StepMark.Current
    this == RoutineStepStatus.DONE -> StepMark.Done
    this == RoutineStepStatus.SKIPPED -> StepMark.Skipped
    else -> StepMark.Pending
}

@Composable
private fun StepRow(label: String, durationSeconds: Int, mark: StepMark) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().crystalCardSurface().padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = when (mark) {
                StepMark.Done -> Icons.Default.CheckCircle
                StepMark.Skipped -> Icons.Default.SkipNext
                StepMark.Current -> Icons.Default.PlayArrow
                StepMark.Pending -> Icons.Default.RadioButtonUnchecked
            },
            contentDescription = mark.name,
            tint = when (mark) {
                StepMark.Done, StepMark.Current -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (mark == StepMark.Current) FontWeight.SemiBold else FontWeight.Normal,
            textDecoration = if (mark == StepMark.Done) TextDecoration.LineThrough else TextDecoration.None,
            color = if (mark == StepMark.Done || mark == StepMark.Skipped) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (durationSeconds > 0) {
            Text(formatStepDuration(durationSeconds), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The last few runs, newest first: when, how long against the plan, and how many steps were done. */
@Composable
private fun RunHistory(runs: List<RoutineRunLog>, clock: (Long) -> String) {
    val zone = TimeZone.currentSystemDefault()
    val today = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(zone).date
    Column(
        modifier = Modifier.fillMaxWidth().crystalCardSurface().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Recent runs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        runs.forEach { log ->
            val took = ((log.finishedAt - log.startedAt) / 1000).toInt().coerceAtLeast(0)
            val day = Instant.fromEpochMilliseconds(log.startedAt).toLocalDateTime(zone).date
            val daysAgo = today.toEpochDays() - day.toEpochDays()
            val dayText = when (daysAgo) {
                0L -> "Today"
                1L -> "Yesterday"
                in 2L..6L -> "$daysAgo days ago"
                else -> "${day.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} ${day.dayOfMonth}"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (log.stepsDone == log.stepsTotal) Icons.Default.CheckCircle else Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = if (log.stepsDone == log.stepsTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("$dayText · ${clock(log.startedAt)}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${log.stepsDone} of ${log.stepsTotal} steps" +
                            if (log.plannedSeconds > 0) " · planned ${formatStepDuration(log.plannedSeconds)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(formatStepDuration(took), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
