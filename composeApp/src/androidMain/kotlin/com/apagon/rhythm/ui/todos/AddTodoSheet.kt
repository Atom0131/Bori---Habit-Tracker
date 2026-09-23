package com.apagon.rhythm.ui.todos
import com.apagon.rhythm.core.time.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.text.format.DateFormat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apagon.rhythm.ui.util.*
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.TodoPriority
import android.media.RingtoneManager
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.ZoneOffset
import com.apagon.rhythm.core.time.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoSheet(
    existing: Todo? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, dueDate: String, priority: TodoPriority, iconIndex: Int, soundUri: String, vibrationPatternId: String) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var selectedIconIndex by remember { mutableStateOf(existing?.iconIndex ?: 0) }
    var selectedPriority by remember {
        mutableStateOf(
            existing?.let {
                TodoPriority.entries.firstOrNull { p -> p.name == it.priority } ?: TodoPriority.NONE
            } ?: TodoPriority.NONE
        )
    }
    var selectedDate by remember {
        mutableStateOf(
            if (existing != null && existing.dueDate.isNotEmpty()) {
                runCatching { LocalDate.parse(existing.dueDate.substringBefore(" ")) }.getOrNull() ?: LocalDate.now()
            } else {
                LocalDate.now()
            }
        )
    }
    var selectedHour by remember {
        mutableIntStateOf(
            existing?.dueDate?.takeIf { it.contains(" ") }?.let {
                runCatching { 
                    val timePart = it.substringAfter(" ")
                    timePart.substringBefore(":").toInt() 
                }.getOrNull()
            } ?: 12
        )
    }
    var selectedMinute by remember {
        mutableIntStateOf(
            existing?.dueDate?.takeIf { it.contains(" ") }?.let {
                runCatching { 
                    val timePart = it.substringAfter(" ")
                    timePart.substringAfter(":").toInt() 
                }.getOrNull()
            } ?: 0
        )
    }
    
    var hasTime by remember { mutableStateOf(existing?.dueDate?.contains(" ") == true) }
    var selectedSoundUri by remember { mutableStateOf(existing?.soundUri ?: "") }
    var selectedPatternId by remember { mutableStateOf(existing?.vibrationPatternId ?: "default") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val is24Hour = remember { context.isSystem24Hour() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .blockSheetBodyDrag(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditorialTitle(
                text = if (existing != null) "Edit To-Do" else "New To-Do",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Icon picker
            TodoIconPickerRow(
                selectedIconIndex = selectedIconIndex,
                onIconSelected = { selectedIconIndex = it }
            )

            FluidTextField(
                value = title,
                onValueChange = { title = it },
                label = "TITLE",
                singleLine = true
            )

            FluidTextField(
                value = note,
                onValueChange = { note = it },
                label = "NOTE (OPTIONAL)",
                singleLine = false,
                maxLines = 3
            )

            // Priority selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PRIORITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TodoPriority.entries.forEach { priority ->
                        val priorityColor = todoPriorityColors[priority]
                        val isSelected = selectedPriority == priority
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPriority = priority },
                            label = {
                                Text(
                                    text = priority.name,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null,
                            colors = if (priority == TodoPriority.NONE || priorityColor == null) {
                                FilterChipDefaults.filterChipColors()
                            } else {
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = priorityColor.copy(alpha = 0.12f),
                                    selectedLabelColor = priorityColor,
                                    selectedLeadingIconColor = priorityColor
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Due date button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 2.dp,
                    tonalElevation = 1.dp,
                    border = BorderStroke(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedDate == LocalDate.now()) "Today" else selectedDate.format(DateTimeFormatter.ofPattern("MMM d")),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Surface(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 2.dp,
                    tonalElevation = 1.dp,
                    border = BorderStroke(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (hasTime) com.apagon.rhythm.ui.util.formatTime(selectedHour, selectedMinute, is24Hour) else "Set Alert",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = hasTime,
                enter = expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(200)),
                exit = shrinkVertically(
                    animationSpec = tween(250, easing = FastOutLinearInEasing)
                ) + fadeOut(tween(150))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoundPickerButton(
                        soundUri = selectedSoundUri,
                        ringtoneType = RingtoneManager.TYPE_NOTIFICATION,
                        onSoundSelected = { selectedSoundUri = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    VibrationPatternButton(
                        patternId = selectedPatternId,
                        onPatternSelected = { selectedPatternId = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            MomentumButton(
                text = if (existing != null) "Update To-Do" else "Create To-Do",
                onClick = {
                    if (hasTime && !hasTechnicalPermissions(context)) {
                        showPermissionDialog = true
                        return@MomentumButton
                    }
                    val dateStr = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val timeStr = if (hasTime) " %02d:%02d".format(selectedHour, selectedMinute) else ""
                    onSave(title.trim(), note.trim(), dateStr + timeStr, selectedPriority, selectedIconIndex, selectedSoundUri, selectedPatternId)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showPermissionDialog) {
        TechnicalPermissionDialog(
            context = context,
            onDismiss = { showPermissionDialog = false }
        )
    }

    if (showDatePicker) {
        val initMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
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
                    hasTime = true
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timeState)
                }
            }
        )
    }
}

@Composable
private fun TodoIconPickerRow(
    selectedIconIndex: Int,
    onIconSelected: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(todoIconList.indices.toList()) { index ->
            val icon = todoIconList[index]
            val isSelected = index == selectedIconIndex
            Surface(
                onClick = { onIconSelected(index) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = if (isSelected) 4.dp else 2.dp,
                tonalElevation = if (isSelected) 2.dp else 1.dp,
                border = if (isSelected) null else BorderStroke(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
