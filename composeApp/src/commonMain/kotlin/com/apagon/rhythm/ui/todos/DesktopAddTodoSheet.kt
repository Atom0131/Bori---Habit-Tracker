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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.material3.IconButton
import com.apagon.rhythm.data.model.Todo
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import com.apagon.rhythm.ui.components.crystalControlElevation
import com.apagon.rhythm.ui.components.crystalControlColor
import com.apagon.rhythm.ui.util.MomentumButton
import com.apagon.rhythm.ui.util.FluidTextField
import com.apagon.rhythm.ui.util.EditorialTitle

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
    /** Non-null opens the sheet as "Edit To-Do", pre-filled (Android's `existing`). */
    existing: Todo? = null,
    /** Shown only when editing: the trash button beside save, behind a confirm, as on Android. */
    onDelete: (() -> Unit)? = null,
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

    val existingDate = remember(existing) { existing?.dueDate?.takeIf { it.isNotEmpty() }?.substringBefore(" ")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
    val existingTime = remember(existing) {
        existing?.dueDate?.takeIf { it.contains(" ") }?.substringAfter(" ")?.split(":")
            ?.let { p -> p.getOrNull(0)?.toIntOrNull()?.let { h -> h to (p.getOrNull(1)?.toIntOrNull() ?: 0) } }
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var priority by remember {
        // Android has H/M/L only and reads anything else as Low.
        mutableStateOf(existing?.let { e -> TodoPriority.entries.firstOrNull { it.name == e.priority && it != TodoPriority.NONE } } ?: TodoPriority.LOW)
    }
    var iconIndex by remember { mutableIntStateOf(existing?.iconIndex ?: 0) }

    var date by remember { mutableStateOf(existingDate ?: LocalDate.now()) }
    var hasTime by remember { mutableStateOf(existingTime != null) }
    var hour by remember { mutableIntStateOf(existingTime?.first ?: 9) }
    var minute by remember { mutableIntStateOf(existingTime?.second ?: 0) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
            // Android's AddTodoSheet layout. As there, a to-do always has a date (today by default)
            // and an optional alert time.
            EditorialTitle(if (existing != null) "Edit To-Do" else "New To-Do", modifier = Modifier.padding(bottom = 8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                items(todoIconList.size) { index ->
                    val isSelected = index == iconIndex
                    Surface(
                        onClick = { iconIndex = index },
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest),
                        shadowElevation = crystalControlElevation(if (isSelected) 4.dp else 2.dp),
                        tonalElevation = crystalControlElevation(if (isSelected) 2.dp else 1.dp),
                        border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                todoIconList[index],
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            FluidTextField(value = title, onValueChange = { title = it }, label = "TITLE")
            FluidTextField(value = note, onValueChange = { note = it }, label = "NOTE (OPTIONAL)", singleLine = false, maxLines = 3)

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PRIORITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TodoPriority.HIGH, TodoPriority.MEDIUM, TodoPriority.LOW).forEach { p ->
                        val priorityColor = PRIORITY_COLORS.getValue(p)
                        val isSelected = priority == p
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = p },
                            label = {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(p.name.take(1), fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest),
                                selectedContainerColor = priorityColor.copy(alpha = 0.12f),
                                selectedLabelColor = priorityColor,
                                selectedLeadingIconColor = priorityColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f).crystalControlSurface(),
                    shape = MaterialTheme.shapes.small,
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(if (date == LocalDate.now()) "Today" else date.format(DateTimeFormatter.ofPattern("MMM d")), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Surface(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f).crystalControlSurface(),
                    shape = MaterialTheme.shapes.small,
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(if (hasTime) formatClock(hour, minute, is24Hour) else "Set Alert", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (existing != null && onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.extraLarge)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete To-Do", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                MomentumButton(
                    text = if (existing != null) "Update To-Do" else "Create To-Do",
                    enabled = title.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val dueDate = date.format(ISO_LOCAL_DATE) + if (hasTime) " %02d:%02d".format(hour, minute) else ""
                        onSave(title, note, dueDate, priority, iconIndex)
                    }
                )
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        RhythmAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete To-Do?") },
            text = { Text("Are you sure you want to delete this to-do? It can be recovered later from Recently Deleted.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
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
                    hasTime = true
                    minute = state.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false; hasTime = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) }
        )
    }
}

/** Android's `todoPriorityColors`. */
private val PRIORITY_COLORS = mapOf(
    TodoPriority.HIGH to Color(0xFFE53935),
    TodoPriority.MEDIUM to Color(0xFFF57C00),
    TodoPriority.LOW to Color(0xFF43A047)
)

private fun formatClock(hour: Int, minute: Int, is24Hour: Boolean): String =
    if (is24Hour) "%02d:%02d".format(hour, minute)
    else "%d:%02d %s".format(when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }, minute, if (hour < 12) "AM" else "PM")
