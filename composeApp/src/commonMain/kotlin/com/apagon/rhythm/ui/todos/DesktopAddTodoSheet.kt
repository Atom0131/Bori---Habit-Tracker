package com.apagon.rhythm.ui.todos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE
import com.apagon.rhythm.data.model.TodoPriority
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalChipSurface
import com.apagon.rhythm.ui.components.crystalControlSurface
import com.apagon.rhythm.ui.components.crystalSelectedChipColor
import com.apagon.rhythm.ui.components.crystalSelectedChipContentColor
import com.apagon.rhythm.ui.components.crystalSwitchColors
import com.apagon.rhythm.ui.components.crystalTextFieldColors
import com.apagon.rhythm.ui.components.crystalTextFieldShape
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import com.apagon.rhythm.ui.util.RhythmDatePickerDialog
import com.apagon.rhythm.ui.util.RhythmSheet
import com.apagon.rhythm.ui.util.todoIconList
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.koin.compose.koinInject

/**
 * Desktop equivalent of androidMain's `AddTodoSheet.kt` — icon, due date/time, and priority, not
 * the title-only dialog this replaces in [DesktopTodayScreen]. Subtasks are deliberately out of
 * scope: unlike habits' checklist items, this app has no `TodoSubtask` table/DAO at all yet (Android's
 * is a separate entity with its own cascading FK and parent/child nesting — a real schema addition,
 * not a UI gap), so there is nothing here for a subtask editor to call into.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAddTodoSheet(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        note: String,
        dueDate: String,
        priority: TodoPriority,
        iconIndex: Int
    ) -> Unit
) {
    val localeFormatting = koinInject<LocaleFormatting>()
    val is24Hour = remember(localeFormatting) { localeFormatting.is24HourFormat() }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TodoPriority.NONE) }
    var iconIndex by remember { mutableIntStateOf(-1) }

    var hasDueDate by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var hasTime by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    RhythmSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("New To-Do", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                colors = crystalTextFieldColors(),
                shape = crystalTextFieldShape(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                colors = crystalTextFieldColors(),
                shape = crystalTextFieldShape(),
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Icon (optional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(todoIconList.size) { index ->
                        val icon = todoIconList[index]
                        val isSelected = iconIndex == index
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .crystalChipSurface(
                                    fill = if (isSelected) crystalSelectedChipColor(MaterialTheme.colorScheme.primary)
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                .clickable { iconIndex = if (isSelected) -1 else index },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isSelected) crystalSelectedChipContentColor(MaterialTheme.colorScheme.onPrimary) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Due date", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = hasDueDate, onCheckedChange = { hasDueDate = it }, colors = crystalSwitchColors())
                }
                if (hasDueDate) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.weight(1f).crystalControlSurface(shape = MaterialTheme.shapes.large).clickable(onClick = { showDatePicker = true }),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(date.format(dateFmt), modifier = Modifier.padding(16.dp))
                        }
                        Row(
                            modifier = Modifier.weight(1f).crystalControlSurface(shape = MaterialTheme.shapes.large).clickable(onClick = {
                                hasTime = true
                                showTimePicker = true
                            }),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(if (hasTime) "%02d:%02d".format(hour, minute) else "Add time", modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PRIORITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TodoPriority.entries.forEach { p ->
                        val selected = priority == p
                        Box(
                            modifier = Modifier
                                .crystalChipSurface(
                                    fill = if (selected) crystalSelectedChipColor(MaterialTheme.colorScheme.primary)
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                .clickable { priority = p }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    p.name.lowercase().replaceFirstChar { it.uppercase() },
                                    color = if (selected) crystalSelectedChipContentColor(MaterialTheme.colorScheme.onPrimary) else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val dueDate = when {
                        !hasDueDate -> ""
                        hasTime -> "${date.format(ISO_LOCAL_DATE)} " + "%02d:%02d".format(hour, minute)
                        else -> date.format(ISO_LOCAL_DATE)
                    }
                    onSave(title, note, dueDate, priority, iconIndex)
                },
                enabled = title.isNotBlank(),
                colors = crystalButtonColors(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create To-Do") }
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
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false; hasTime = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) }
        )
    }
}
