package com.apagon.rhythm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Recurrence
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.ui.util.RhythmDatePickerDialog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/**
 * **"Repeat: Never ▸"** — the one repeat control, used by the event, to-do and reminder editors.
 * [rule] is a [Recurrence] string (`""` = doesn't repeat); [start] is the item's date, which the
 * presets are worded around ("Weekly on Tue"). Android has the same control (`ui/components/RepeatPicker.kt`).
 */
@Composable
fun RepeatRow(rule: String, start: LocalDate, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var showChooser by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    val parsed = Recurrence.parse(rule)
    Surface(
        onClick = { showChooser = true },
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth().crystalControlSurface()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Repeat", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(
                parsed?.describe(start) ?: "Never",
                style = MaterialTheme.typography.labelLarge,
                color = if (parsed != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
    if (showChooser) {
        RepeatChooserDialog(
            rule = rule, start = start,
            onPick = { onChange(it); showChooser = false },
            onCustom = { showChooser = false; showCustom = true },
            onDismiss = { showChooser = false }
        )
    }
    if (showCustom) {
        RepeatCustomDialog(
            initial = parsed, start = start,
            onDone = { onChange(it); showCustom = false },
            onDismiss = { showCustom = false }
        )
    }
}

/** The presets, worded around [start]. A rule that matches none of them shows as a checked "Custom". */
fun repeatPresets(start: LocalDate): List<Pair<String, String>> {
    val day = start.dayOfWeek.isoDayNumber
    return listOf(
        "" to "Never",
        "FREQ=DAILY" to "Daily",
        "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR" to "Every weekday",
        "FREQ=WEEKLY" to Recurrence(Recurrence.Freq.WEEKLY, byDay = setOf(day)).describe(start),
        "FREQ=MONTHLY" to Recurrence(Recurrence.Freq.MONTHLY).describe(start),
        "FREQ=YEARLY" to Recurrence(Recurrence.Freq.YEARLY).describe(start)
    )
}

@Composable
private fun RepeatChooserDialog(
    rule: String, start: LocalDate,
    onPick: (String) -> Unit, onCustom: () -> Unit, onDismiss: () -> Unit
) {
    val current = Recurrence.parse(rule)?.copy(anchor = null)?.format() ?: ""
    val presets = repeatPresets(start)
    val isCustom = presets.none { it.first == current }
    RhythmAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repeat") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                presets.forEach { (value, label) ->
                    ChoiceRow(label, selected = value == current) {
                        // Unchanged keeps the stored rule as is (a to-do's anchor survives).
                        onPick(if (value == current) rule else value)
                    }
                }
                ChoiceRow(
                    if (isCustom) "Custom: ${Recurrence.parse(rule)?.describe(start)}" else "Custom…",
                    selected = isCustom, onClick = onCustom
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
    }
}

private enum class EndMode { NEVER, ON_DATE, AFTER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatCustomDialog(
    initial: Recurrence?, start: LocalDate, onDone: (String) -> Unit, onDismiss: () -> Unit
) {
    var freq by remember { mutableStateOf(initial?.freq ?: Recurrence.Freq.WEEKLY) }
    var interval by remember { mutableStateOf((initial?.interval ?: 1).toString()) }
    var days by remember { mutableStateOf(initial?.byDay?.ifEmpty { null } ?: setOf(start.dayOfWeek.isoDayNumber)) }
    var endMode by remember {
        mutableStateOf(when { initial?.until != null -> EndMode.ON_DATE; initial?.count != null -> EndMode.AFTER; else -> EndMode.NEVER })
    }
    var until by remember { mutableStateOf(initial?.until ?: start.plusMonths(1)) }
    var count by remember { mutableStateOf((initial?.count ?: 10).toString()) }
    var pickUntil by remember { mutableStateOf(false) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    val n = interval.toIntOrNull()?.takeIf { it in 1..999 }
    val c = count.toIntOrNull()?.takeIf { it in 1..999 }
    val valid = n != null && (endMode != EndMode.AFTER || c != null) && (freq != Recurrence.Freq.WEEKLY || days.isNotEmpty())

    RhythmAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom repeat") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Every", style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = interval, onValueChange = { v -> interval = v.filter { it.isDigit() }.take(3) },
                        singleLine = true, modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val plural = (n ?: 1) != 1
                    listOf(
                        Recurrence.Freq.DAILY to "day", Recurrence.Freq.WEEKLY to "week",
                        Recurrence.Freq.MONTHLY to "month", Recurrence.Freq.YEARLY to "year"
                    ).forEach { (f, label) ->
                        FilterChip(selected = freq == f, onClick = { freq = f }, label = { Text(if (plural) label + "s" else label) })
                    }
                }
                if (freq == Recurrence.Freq.WEEKLY) {
                    Text("On", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, label ->
                            val d = i + 1
                            FilterChip(
                                selected = d in days,
                                onClick = { days = if (d in days) days - d else days + d },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                Text("Ends", style = MaterialTheme.typography.labelLarge)
                EndRow("Never", endMode == EndMode.NEVER) { endMode = EndMode.NEVER }
                EndRow("On ${until.format(dateFmt)}", endMode == EndMode.ON_DATE) { endMode = EndMode.ON_DATE; pickUntil = true }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = endMode == EndMode.AFTER, onClick = { endMode = EndMode.AFTER })
                    Text("After", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = count, onValueChange = { v -> count = v.filter { it.isDigit() }.take(3); endMode = EndMode.AFTER },
                        singleLine = true, modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("times", style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val r = Recurrence(
                    freq = freq, interval = n ?: 1,
                    byDay = if (freq == Recurrence.Freq.WEEKLY) days else emptySet(),
                    until = until.takeIf { endMode == EndMode.ON_DATE },
                    count = c.takeIf { endMode == EndMode.AFTER }
                )
                onDone(r.format())
            }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
    if (pickUntil) {
        val state = rememberDatePickerState(initialSelectedDateMillis = until.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        RhythmDatePickerDialog(
            onDismissRequest = { pickUntil = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { until = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickUntil = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickUntil = false }) { Text("Cancel") } }
        ) { DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Color.Transparent)) }
    }
}

@Composable
private fun EndRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * "Only this event / All events" — asked when saving or deleting one date of a repeating event.
 * [onChoose] gets true for only this date.
 */
@Composable
fun SeriesScopeDialog(deleting: Boolean, onChoose: (onlyThis: Boolean) -> Unit, onDismiss: () -> Unit) {
    RhythmAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (deleting) "Delete repeating event" else "Change repeating event") },
        text = {
            Column {
                ChoiceRow("Only this event", selected = false) { onChoose(true) }
                ChoiceRow("All events", selected = false) { onChoose(false) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** The small ↻ beside a repeating item in a list. */
@Composable
fun RepeatBadge() {
    Icon(
        Icons.Default.Repeat, contentDescription = "Repeats",
        modifier = Modifier.size(14.dp),
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    )
}

/** "↻ Weekly on Tue" under a repeating to-do's or reminder's title; nothing when it doesn't repeat. */
@Composable
fun RepeatLine(rule: String, date: String) {
    val r = Recurrence.parse(rule) ?: return
    val start = runCatching { LocalDate.parse(date.take(10)) }.getOrNull()
    Row(verticalAlignment = Alignment.CenterVertically) {
        RepeatBadge()
        Spacer(Modifier.width(4.dp))
        Text(r.describe(start), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
