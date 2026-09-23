package com.apagon.rhythm.ui.reminders
import com.apagon.rhythm.core.time.*

import android.media.RingtoneManager
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.ui.util.*
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.ZoneOffset
import com.apagon.rhythm.core.time.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderSheet(
    existing: Reminder? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, dateTime: String, soundUri: String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val is24Hour = remember { context.isSystem24Hour() }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var selectedSoundUri by remember { mutableStateOf(existing?.soundUri ?: "") }

    val (initDate, initHour, initMinute) = remember(existing) {
        if (existing != null) {
            try {
                val parts = existing.dateTime.split(" ")
                val datePart = LocalDate.parse(parts[0])
                val timeParts = parts[1].split(":")
                Triple(datePart, timeParts[0].toInt(), timeParts[1].toInt())
            } catch (e: Exception) {
                Triple(LocalDate.now(), 8, 0)
            }
        } else {
            Triple(LocalDate.now(), 8, 0)
        }
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(if (existing != null) initDate else null) }
    var selectedHour by remember { mutableStateOf(initHour) }
    var selectedMinute by remember { mutableStateOf(initMinute) }
    var timeSet by remember { mutableStateOf(existing != null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .blockSheetBodyDrag(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            EditorialTitle(
                text = if (existing != null) "Refine Reminder" else "New Reminder",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FluidTextField(
                value = title,
                onValueChange = { title = it },
                label = "WHAT'S THE TASK?",
                singleLine = true
            )

            FluidTextField(
                value = note,
                onValueChange = { note = it },
                label = "ADDITIONAL NOTES",
                singleLine = false,
                maxLines = 3
            )

            // Date/Time Selection Area
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "WHEN SHOULD WE NOTIFY YOU?",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = selectedDate?.format(DateTimeFormatter.ofPattern("MMM d"))
                                    ?: "Pick Date",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Surface(
                        onClick = { showTimePicker = true },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AccessTime, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (timeSet) formatTime(selectedHour, selectedMinute, is24Hour)
                                else "Pick Time",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            SoundPickerButton(
                soundUri = selectedSoundUri,
                ringtoneType = RingtoneManager.TYPE_NOTIFICATION,
                onSoundSelected = { selectedSoundUri = it }
            )

            val canSave = title.isNotBlank() && selectedDate != null && timeSet
            MomentumButton(
                text = if (existing != null) "Update Reminder" else "Create Reminder",
                onClick = {
                    val dateStr = selectedDate!!.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val timeStr = "%02d:%02d".format(selectedHour, selectedMinute)
                    onSave(title.trim(), note.trim(), "$dateStr $timeStr", selectedSoundUri)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDatePicker) {
        val initMillis = (selectedDate ?: LocalDate.now())
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = is24Hour
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    selectedHour = timeState.hour
                    selectedMinute = timeState.minute
                    timeSet = true
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}
