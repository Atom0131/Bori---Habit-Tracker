package com.apagon.rhythm.ui.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalControlSurface
import com.apagon.rhythm.ui.components.crystalTextFieldColors
import com.apagon.rhythm.ui.components.crystalTextFieldShape
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import com.apagon.rhythm.ui.util.RhythmDatePickerDialog
import com.apagon.rhythm.ui.util.RhythmSheet
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.koin.compose.koinInject
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import com.apagon.rhythm.ui.util.formatClockTime
import com.apagon.rhythm.ui.util.MomentumButton
import com.apagon.rhythm.ui.util.FluidTextField
import com.apagon.rhythm.ui.util.EditorialTitle

// Desktop counterpart to Android's AddReminderSheet.kt, built from the same editor pieces.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAddReminderSheet(
    existing: Reminder? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, dateTime: String) -> Unit
) {
    val localeFormatting = koinInject<LocaleFormatting>()
    val is24Hour = remember(localeFormatting) { localeFormatting.is24HourFormat() }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember { mutableStateOf(existing?.dateTime?.substringBefore(" ")?.let { runCatching { LocalDate.parse(it, ISO_LOCAL_DATE) }.getOrNull() } ?: LocalDate.now()) }
    var hour by remember { mutableIntStateOf(existing?.dateTime?.substringAfter(" ")?.substringBefore(":")?.toIntOrNull() ?: 9) }
    var minute by remember { mutableIntStateOf(existing?.dateTime?.substringAfter(":")?.toIntOrNull() ?: 0) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // As Android, a new reminder starts with no time chosen ("Pick Time") and can't be saved until
    // one is.
    var timeSet by remember { mutableStateOf(existing != null) }

    // Android's AddReminderSheet layout (its sound picker is phone-only).
    RhythmSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            EditorialTitle(if (existing != null) "Refine Reminder" else "New Reminder", modifier = Modifier.padding(bottom = 8.dp))
            FluidTextField(value = title, onValueChange = { title = it }, label = "WHAT'S THE TASK?")
            FluidTextField(value = note, onValueChange = { note = it }, label = "ADDITIONAL NOTES", singleLine = false, maxLines = 3)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WHEN SHOULD WE NOTIFY YOU?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = MaterialTheme.shapes.small,
                        color = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).crystalControlSurface()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(if (date == LocalDate.now()) "Today" else date.format(DateTimeFormatter.ofPattern("MMM d")), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Surface(
                        onClick = { showTimePicker = true },
                        shape = MaterialTheme.shapes.small,
                        color = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).crystalControlSurface()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.AccessTime, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(if (timeSet) formatClockTime(hour, minute, is24Hour) else "Pick Time", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            MomentumButton(
                text = if (existing != null) "Update Reminder" else "Create Reminder",
                enabled = title.isNotBlank() && timeSet,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val dateTime = "${date.format(ISO_LOCAL_DATE)} " + "%02d:%02d".format(hour, minute)
                    onSave(title.trim(), note.trim(), dateTime)
                }
            )
        }
    }

    if (showDatePicker) {
        val initMillis = date.atStartOfDay(TimeZone.UTC).toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        RhythmDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(TimeZone.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Color.Transparent)) }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = is24Hour)
        RhythmAlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = state.hour
                    minute = state.minute
                    timeSet = true
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) }
        )
    }
}
