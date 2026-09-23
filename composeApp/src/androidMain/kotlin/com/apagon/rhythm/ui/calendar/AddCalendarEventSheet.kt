package com.apagon.rhythm.ui.calendar
import com.apagon.rhythm.core.time.*

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.repository.DeviceCalendar
import com.apagon.rhythm.ui.util.*
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.ZoneId
import com.apagon.rhythm.core.time.ZoneOffset
import com.apagon.rhythm.core.time.DateTimeFormatter
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalendarEventSheet(
    existing: CalendarEvent? = null,
    initialDate: LocalDate = LocalDate.now(),
    isPro: Boolean = true,
    availableCalendars: List<DeviceCalendar> = emptyList(),
    onDismiss: () -> Unit,
    onShowPaywall: (String) -> Unit = {},
    onSave: (CalendarEvent, Long?) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val is24Hour = remember { context.isSystem24Hour() }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    
    var targetCalendarId by remember { mutableStateOf<Long?>(0L) } // 0 = Rhythm Local
    var showCalendarPicker by remember { mutableStateOf(false) }

    var startDate by remember { 
        mutableStateOf<LocalDate?>(
            existing?.startDate?.let { LocalDate.parse(it, ISO_LOCAL_DATE) } ?: initialDate
        )
    }
    var endDate by remember { 
        mutableStateOf<LocalDate?>(
            existing?.endDate?.let { LocalDate.parse(it, ISO_LOCAL_DATE) } ?: initialDate
        )
    }

    var allDay by remember { mutableStateOf(existing?.startTime == null && existing != null || (existing == null && false)) }
    if (existing != null && allDay == false && existing.startTime == null) { allDay = true }

    var startHour by remember { 
        mutableIntStateOf(
            existing?.startTime?.substringBefore(":")?.toIntOrNull() ?: 9
        )
    }
    var startMinute by remember { 
        mutableIntStateOf(
            existing?.startTime?.substringAfter(":")?.toIntOrNull() ?: 0
        )
    }
    var endHour by remember { 
        mutableIntStateOf(
            existing?.endTime?.substringBefore(":")?.toIntOrNull() ?: 10
        )
    }
    var endMinute by remember { 
        mutableIntStateOf(
            existing?.endTime?.substringAfter(":")?.toIntOrNull() ?: 0
        )
    }
    
    var colorIndex by remember { mutableIntStateOf(existing?.colorIndex ?: 0) }
    var colorArgb by remember { mutableStateOf<Int?>(existing?.colorArgb) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditorialTitle(
                text = if (existing == null) "New Event" else "Edit Event",
                modifier = Modifier.padding(bottom = 8.dp)
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

            // Date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { showStartDatePicker = true },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 2.dp,
                    tonalElevation = 1.dp,
                    border = BorderStroke(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(startDate?.format(dateFmt) ?: "Start Date", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Surface(
                    onClick = { showEndDatePicker = true },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 2.dp,
                    tonalElevation = 1.dp,
                    border = BorderStroke(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(endDate?.format(dateFmt) ?: "End Date", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // All-day toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "All day",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = allDay, onCheckedChange = { allDay = it })
            }

            // Time pickers — only visible when not all-day
            AnimatedVisibility(
                visible = !allDay,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(tween(200)),
                exit = shrinkVertically(
                    animationSpec = tween(250, easing = FastOutLinearInEasing)
                ) + fadeOut(tween(150))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { showStartTimePicker = true },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shadowElevation = 2.dp,
                        tonalElevation = 1.dp,
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(formatTime(startHour, startMinute, is24Hour), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Surface(
                        onClick = { showEndTimePicker = true },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shadowElevation = 2.dp,
                        tonalElevation = 1.dp,
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(formatTime(endHour, endMinute, is24Hour), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Target Calendar selector (only for new events)
            if (existing == null && availableCalendars.isNotEmpty()) {
                Text("TARGET CALENDAR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val selectedCalendar = availableCalendars.find { it.id == targetCalendarId }
                Surface(
                    onClick = { showCalendarPicker = true },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = 2.dp,
                    tonalElevation = 1.dp,
                    border = BorderStroke(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(selectedCalendar?.displayName ?: "Rhythm (Local Only)", style = MaterialTheme.typography.labelLarge)
                        Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            }

            // Color picker
            Text("Color", style = MaterialTheme.typography.labelLarge)
            ColorPickerRow(
                colorIndex = colorIndex,
                colorArgb = colorArgb,
                isPro = isPro,
                onColorSelected = { idx, argb ->
                    colorIndex = idx
                    colorArgb = argb
                },
                onShowPaywall = onShowPaywall
            )

            Spacer(modifier = Modifier.height(4.dp))

            val canSave = title.isNotBlank() && startDate != null && endDate != null
            MomentumButton(
                text = if (existing == null) "Create Event" else "Update Event",
                onClick = {
                    val sd = startDate!!
                    val ed = if (endDate!! < sd) sd else endDate!!
                    onSave(
                        CalendarEvent(
                            id = existing?.id ?: 0,
                            title = title.trim(),
                            note = note.trim(),
                            startDate = sd.format(ISO_LOCAL_DATE),
                            endDate = ed.format(ISO_LOCAL_DATE),
                            startTime = if (allDay) null else "%02d:%02d".format(startHour, startMinute),
                            endTime = if (allDay) null else "%02d:%02d".format(endHour, endMinute),
                            colorIndex = colorIndex,
                            colorArgb = colorArgb
                        ),
                        targetCalendarId
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Calendar Picker Dialog
    if (showCalendarPicker) {
        AlertDialog(
            onDismissRequest = { showCalendarPicker = false },
            title = { Text("Select Calendar") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Rhythm (Local Only)") },
                        modifier = Modifier.clickable {
                            targetCalendarId = 0L
                            showCalendarPicker = false
                        },
                        trailingContent = { if (targetCalendarId == 0L) Icon(androidx.compose.material.icons.Icons.Default.Check, null) }
                    )
                    availableCalendars.forEach { calendar ->
                        ListItem(
                            headlineContent = { Text(calendar.displayName) },
                            supportingContent = { Text(calendar.accountName) },
                            modifier = Modifier.clickable {
                                targetCalendarId = calendar.id
                                showCalendarPicker = false
                            },
                            trailingContent = { if (targetCalendarId == calendar.id) Icon(androidx.compose.material.icons.Icons.Default.Check, null) }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCalendarPicker = false }) { Text("Close") } }
        )
    }

    // Start date picker
    if (showStartDatePicker) {
        val initMillis = (startDate ?: LocalDate.now())
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        startDate = picked
                        if (endDate != null && endDate!! < picked) endDate = picked
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state, colors = DatePickerDefaults.colors(todayDateBorderColor = Color.Transparent)) }
    }

    // End date picker
    if (showEndDatePicker) {
        val initMillis = (endDate ?: startDate ?: LocalDate.now())
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        endDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state, colors = DatePickerDefaults.colors(todayDateBorderColor = Color.Transparent)) }
    }

    // Start time picker
    if (showStartTimePicker) {
        val state = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute, is24Hour = is24Hour)
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    startHour = state.hour
                    startMinute = state.minute
                    
                    // Auto-advance end time by 30 mins
                    var totalMinutes = startHour * 60 + startMinute + 30
                    val newEndHour = (totalMinutes / 60) % 24
                    val newEndMinute = totalMinutes % 60
                    
                    endHour = newEndHour
                    endMinute = newEndMinute
                    
                    // If it rolls over to the next day, advance end date if it matches start date
                    if (totalMinutes >= 24 * 60 && endDate == startDate) {
                        endDate = endDate?.plusDays(1)
                    }
                    
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) }
        )
    }

    // End time picker
    if (showEndTimePicker) {
        val state = rememberTimePickerState(initialHour = endHour, initialMinute = endMinute, is24Hour = is24Hour)
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    endHour = state.hour
                    endMinute = state.minute
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) }
        )
    }
}
