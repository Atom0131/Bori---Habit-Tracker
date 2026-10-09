package com.apagon.rhythm.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalControlSurface
import com.apagon.rhythm.ui.components.crystalSwitchColors
import com.apagon.rhythm.ui.components.crystalTextFieldColors
import com.apagon.rhythm.ui.components.crystalTextFieldShape
import com.apagon.rhythm.ui.util.ColorPickerRow
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import com.apagon.rhythm.ui.util.RhythmDatePickerDialog
import com.apagon.rhythm.ui.util.RhythmSheet
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.apagon.rhythm.ui.util.EditorSection
import com.apagon.rhythm.ui.util.MomentumButton
import com.apagon.rhythm.ui.util.FluidTextField
import com.apagon.rhythm.ui.util.EditorialTitle

/**
 * Desktop equivalent of androidMain's AddCalendarEventSheet.kt, with the
 * target-calendar picker, isPro/paywall gating, and custom color-wheel
 * picker dropped — see ref_notes/plan_2026-09-24_calendar_port.md decisions
 * 6-7. Date/time logic is otherwise a near-verbatim port; it already ran
 * against commonMain's java.time compat shim before this stage.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun DesktopAddCalendarEventSheet(
    existing: CalendarEvent? = null,
    initialDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val localeFormatting = koinInject<LocaleFormatting>()
    val is24Hour = remember(localeFormatting) { localeFormatting.is24HourFormat() }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    var startDate by remember {
        mutableStateOf<LocalDate?>(existing?.startDate?.let { LocalDate.parse(it, ISO_LOCAL_DATE) } ?: initialDate)
    }
    var endDate by remember {
        mutableStateOf<LocalDate?>(existing?.endDate?.let { LocalDate.parse(it, ISO_LOCAL_DATE) } ?: initialDate)
    }

    var allDay by remember { mutableStateOf(existing != null && existing.startTime == null) }

    var startHour by remember { mutableIntStateOf(existing?.startTime?.substringBefore(":")?.toIntOrNull() ?: 9) }
    var startMinute by remember { mutableIntStateOf(existing?.startTime?.substringAfter(":")?.toIntOrNull() ?: 0) }
    var endHour by remember { mutableIntStateOf(existing?.endTime?.substringBefore(":")?.toIntOrNull() ?: 10) }
    var endMinute by remember { mutableIntStateOf(existing?.endTime?.substringAfter(":")?.toIntOrNull() ?: 0) }

    var colorIndex by remember { mutableIntStateOf(existing?.colorIndex ?: 0) }
    var colorArgb by remember { mutableStateOf<Int?>(existing?.colorArgb) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    RhythmSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        fillHeight = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Android's AddCalendarEventSheet layout: TITLE, NOTE, a WHEN panel, colour, glass button.
            EditorialTitle(if (existing == null) "New Event" else "Edit Event", modifier = Modifier.padding(bottom = 8.dp))

            FluidTextField(value = title, onValueChange = { title = it }, label = "TITLE")
            FluidTextField(value = note, onValueChange = { note = it }, label = "NOTE (OPTIONAL)", singleLine = false, maxLines = 3)

            EditorSection(label = "WHEN") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickerSurface(
                    label = startDate?.format(dateFmt) ?: "Start Date",
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
                PickerSurface(
                    label = endDate?.format(dateFmt) ?: "End Date",
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("All day", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Switch(checked = allDay, onCheckedChange = { allDay = it }, colors = crystalSwitchColors())
            }

            AnimatedVisibility(
                visible = !allDay,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(tween(200)),
                exit = shrinkVertically(animationSpec = tween(250, easing = FastOutLinearInEasing)) + fadeOut(tween(150))
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerSurface(
                        label = formatTimeLabel("%02d:%02d".format(startHour, startMinute), is24Hour),
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    PickerSurface(
                        label = formatTimeLabel("%02d:%02d".format(endHour, endMinute), is24Hour),
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            } // end EditorSection("WHEN")

            Text("Color", style = MaterialTheme.typography.labelLarge)
            ColorPickerRow(
                colorIndex = colorIndex,
                colorArgb = colorArgb,
                onColorSelected = { idx, argb -> colorIndex = idx; colorArgb = argb },
                viewModel = koinViewModel()
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
                            colorArgb = colorArgb,
                            // Carry the existing row's sync identity/creation time forward on an
                            // edit — otherwise these fall through to the data class's fresh-mint
                            // defaults and an edit mints a brand-new syncId, which the sync engine
                            // then treats as a duplicate new event on a peer instead of an update.
                            syncId = existing?.syncId ?: Uuid.random().toString(),
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showStartDatePicker) {
        val initMillis = (startDate ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        RhythmDatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        startDate = picked
                        if (endDate != null && endDate!! < picked) endDate = picked
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Color.Transparent, todayDateBorderColor = Color.Transparent)) }
    }

    if (showEndDatePicker) {
        val initMillis = (endDate ?: startDate ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        RhythmDatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        endDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Color.Transparent, todayDateBorderColor = Color.Transparent)) }
    }

    if (showStartTimePicker) {
        val state = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute, is24Hour = is24Hour)
        RhythmAlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    startHour = state.hour
                    startMinute = state.minute
                    var totalMinutes = startHour * 60 + startMinute + 30
                    endHour = (totalMinutes / 60) % 24
                    endMinute = totalMinutes % 60
                    if (totalMinutes >= 24 * 60 && endDate == startDate) endDate = endDate?.plusDays(1)
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) }
        )
    }

    if (showEndTimePicker) {
        val state = rememberTimePickerState(initialHour = endHour, initialMinute = endMinute, is24Hour = is24Hour)
        RhythmAlertDialog(
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

@Composable
private fun PickerSurface(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.crystalControlSurface().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
