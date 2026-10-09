package com.apagon.rhythm.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalSwitchColors
import com.apagon.rhythm.ui.components.crystalTextFieldColors
import com.apagon.rhythm.ui.components.crystalTextFieldShape
import com.apagon.rhythm.ui.util.RhythmSheet
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.Icons
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.apagon.rhythm.ui.components.crystalIconButtonSurface
import com.apagon.rhythm.ui.components.crystalSelectedChipContentColor
import com.apagon.rhythm.ui.components.crystalSelectedChipColor
import com.apagon.rhythm.ui.components.crystalChipSurface
import com.apagon.rhythm.ui.util.crystalSegmentedButtonColors
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import com.apagon.rhythm.ui.util.PickerSummaryCard
import com.apagon.rhythm.ui.util.MomentumButton
import com.apagon.rhythm.ui.util.FluidTextField
import com.apagon.rhythm.ui.util.EditorialTitle

private enum class DurationTarget(val title: String) { TIMER("Duration"), WORK("Work"), SHORT("Short Break"), LONG("Long Break") }

private fun hms(total: Int) = Triple(total / 3600, (total % 3600) / 60, total % 60)
private fun fmt(total: Int): String { val (h, m, sec) = hms(total); return "%02d:%02d:%02d".format(h, m, sec) }

/**
 * Port of Android's AddTimerSheet: Timer / Pomodoro segmented switch, durations shown as
 * PickerSummaryCards that open an h/m/s dialog, Pomodoro's Work / Short Break / Long Break tabs and
 * the sessions stepper, then LABEL. Sound and vibration pickers are phone-only. All durations are
 * in seconds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAddTimerSheet(
    existing: Timer? = null,
    initialPomo: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (label: String, durationSeconds: Int) -> Unit,
    onSavePomo: (label: String, workSecs: Int, shortBreakSecs: Int, longBreakSecs: Int, sessions: Int) -> Unit
) {
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var isPomo by remember { mutableStateOf(existing?.isPomo ?: initialPomo) }
    var timerSecs by remember { mutableIntStateOf(existing?.takeIf { !it.isPomo }?.durationSeconds ?: 300) }
    var workSecs by remember { mutableIntStateOf(existing?.pomoWorkSecs?.takeIf { it > 0 } ?: 1500) }
    var shortSecs by remember { mutableIntStateOf(existing?.pomoShortBreakSecs?.takeIf { it > 0 } ?: 300) }
    var longSecs by remember { mutableIntStateOf(existing?.pomoLongBreakSecs?.takeIf { it > 0 } ?: 900) }
    var sessions by remember { mutableIntStateOf(existing?.pomoSessionsPerRound?.takeIf { it > 0 } ?: 4) }
    var editing by remember { mutableStateOf<DurationTarget?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val canSave = if (isPomo) workSecs > 0 else timerSecs > 0

    RhythmSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            EditorialTitle(
                if (existing != null) { if (isPomo) "Edit Pomodoro" else "Edit Timer" } else { if (isPomo) "New Pomodoro" else "New Timer" },
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(selected = !isPomo, onClick = { isPomo = false }, shape = SegmentedButtonDefaults.itemShape(0, 2), colors = crystalSegmentedButtonColors(), icon = {}) { Text("Timer") }
                SegmentedButton(selected = isPomo, onClick = { isPomo = true }, shape = SegmentedButtonDefaults.itemShape(1, 2), colors = crystalSegmentedButtonColors(), icon = {}) { Text("Pomodoro") }
            }
            if (!isPomo) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DURATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PickerSummaryCard(icon = Icons.Default.HourglassTop, value = fmt(timerSecs), onClick = { editing = DurationTarget.TIMER })
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val tabs = listOf("Work", "Short Break", "Long Break")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tabs.forEachIndexed { index, title ->
                            val selected = selectedTab == index
                            Box(
                                modifier = Modifier.weight(1f)
                                    .then(if (selected) Modifier.crystalChipSurface(crystalSelectedChipColor(MaterialTheme.colorScheme.secondaryContainer)) else Modifier)
                                    .clip(CircleShape)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    color = if (selected) crystalSelectedChipContentColor(MaterialTheme.colorScheme.onSecondaryContainer) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    when (selectedTab) {
                        0 -> PickerSummaryCard(icon = Icons.Default.HourglassTop, value = fmt(workSecs), onClick = { editing = DurationTarget.WORK })
                        1 -> PickerSummaryCard(icon = Icons.Default.HourglassTop, value = fmt(shortSecs), onClick = { editing = DurationTarget.SHORT })
                        else -> PickerSummaryCard(icon = Icons.Default.HourglassTop, value = fmt(longSecs), onClick = { editing = DurationTarget.LONG })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SESSIONS PER ROUND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (sessions > 1) sessions-- }, enabled = sessions > 1, modifier = Modifier.crystalIconButtonSurface()) {
                                Icon(Icons.Default.Remove, contentDescription = "Fewer sessions")
                            }
                            Text(sessions.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                            IconButton(onClick = { if (sessions < 12) sessions++ }, enabled = sessions < 12, modifier = Modifier.crystalIconButtonSurface()) {
                                Icon(Icons.Default.Add, contentDescription = "More sessions")
                            }
                        }
                    }
                }
            }
            FluidTextField(value = label, onValueChange = { label = it }, label = "LABEL (OPTIONAL)")
            MomentumButton(
                text = if (existing != null) "Update" else if (isPomo) "Create Pomodoro" else "Create Timer",
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (isPomo) onSavePomo(label.trim(), workSecs, shortSecs, longSecs, sessions)
                    else onSave(label.trim(), timerSecs)
                }
            )
        }
    }

    editing?.let { target ->
        val current = when (target) {
            DurationTarget.TIMER -> timerSecs; DurationTarget.WORK -> workSecs
            DurationTarget.SHORT -> shortSecs; DurationTarget.LONG -> longSecs
        }
        DurationPickerDialog(target.title, current, onDismiss = { editing = null }) { secs ->
            when (target) {
                DurationTarget.TIMER -> timerSecs = secs; DurationTarget.WORK -> workSecs = secs
                DurationTarget.SHORT -> shortSecs = secs; DurationTarget.LONG -> longSecs = secs
            }
            editing = null
        }
    }
}

/** Hours / minutes / seconds entry, the desktop stand-in for Android's wheel-based
 * RhythmDurationPickerDialog. */
@Composable
private fun DurationPickerDialog(title: String, initialSeconds: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val (h0, m0, s0) = hms(initialSeconds)
    var h by remember { mutableStateOf(h0.toString()) }
    var m by remember { mutableStateOf(m0.toString()) }
    var sec by remember { mutableStateOf(s0.toString()) }
    fun clean(v: String, max: Int) = v.filter { it.isDigit() }.take(2).let { t -> t.toIntOrNull()?.coerceAtMost(max)?.toString() ?: t }
    RhythmAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                FluidTextField(h, { h = clean(it, 23) }, "HOURS", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                FluidTextField(m, { m = clean(it, 59) }, "MINUTES", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                FluidTextField(sec, { sec = clean(it, 59) }, "SECONDS", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm((h.toIntOrNull() ?: 0) * 3600 + (m.toIntOrNull() ?: 0) * 60 + (sec.toIntOrNull() ?: 0)) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
