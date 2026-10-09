package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalChipSurface
import com.apagon.rhythm.ui.components.crystalControlSurface
import com.apagon.rhythm.ui.components.crystalSelectedChipColor
import com.apagon.rhythm.ui.components.crystalSelectedChipContentColor
import com.apagon.rhythm.ui.components.crystalSwitchColors
import com.apagon.rhythm.ui.components.crystalTextFieldColors
import com.apagon.rhythm.ui.components.crystalTextFieldShape
import com.apagon.rhythm.ui.util.ColorPickerRow
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import com.apagon.rhythm.ui.util.RhythmSheet
import com.apagon.rhythm.ui.util.habitIconLibrary
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.LaunchedEffect
import kotlinx.datetime.daysUntil
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.now
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Surface
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.apagon.rhythm.ui.components.crystalCheckboxColors
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import com.apagon.rhythm.ui.util.crystalFilterChipColors
import com.apagon.rhythm.ui.util.crystalSegmentedButtonColors
import com.apagon.rhythm.ui.util.DayCircle
import com.apagon.rhythm.ui.util.WeekDayPicker
import com.apagon.rhythm.ui.util.IconPickerButton
import com.apagon.rhythm.ui.util.MomentumButton
import com.apagon.rhythm.ui.util.EditorSection
import com.apagon.rhythm.ui.util.FluidTextField
import com.apagon.rhythm.ui.util.EditorialTitle

private val DURATION_UNITS = listOf("Days", "Weeks", "Months")
private const val EVERY_WEEKDAY_MASK = 0b111_1111

/**
 * Desktop equivalent of androidMain's `AddHabitSheet.kt` — full field coverage (schedule, color,
 * icon, checklist, reminder), not the name-only dialog this replaces in [DesktopTodayScreen]. Not
 * byte-identical: the segmented Daily/Weekly/Monthly picker and the day grids are built from the
 * same crystal chip-row primitives [DesktopAddAlarmSheet]'s day picker already established, rather
 * than `SingleChoiceSegmentedButtonRow` (no crystal-themed colors exist yet for that component).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAddHabitSheet(
    onDismiss: () -> Unit,
    /** Non-null opens the sheet as "Edit Habit", pre-filled from this habit (Android's `initialHabit`). */
    initialHabit: Habit? = null,
    initialChecklist: List<String> = emptyList(),
    onSave: (
        name: String,
        description: String,
        frequency: HabitFrequency,
        weekDaysMask: Int,
        monthDaysMask: Int,
        isChecklist: Boolean,
        checklistItems: List<String>,
        colorIndex: Int,
        colorArgb: Int?,
        durationDays: Int,
        iconIndex: Int,
        reminderTime: String?
    ) -> Unit
) {
    val localeFormatting = koinInject<LocaleFormatting>()
    val is24Hour = remember(localeFormatting) { localeFormatting.is24HourFormat() }

    val h = initialHabit
    val initialDurationUnit = remember(h?.durationDays) {
        val d = h?.durationDays ?: 0
        when {
            d > 0 && d % 30 == 0 -> "Months"
            d > 0 && d % 7 == 0 -> "Weeks"
            else -> "Days"
        }
    }
    val initialReminder = remember(h?.reminderTime) {
        h?.reminderTime?.split(":")?.let { parts -> parts.getOrNull(0)?.toIntOrNull()?.let { hh -> hh to (parts.getOrNull(1)?.toIntOrNull() ?: 0) } }
    }

    var name by remember { mutableStateOf(h?.name ?: "") }
    var description by remember { mutableStateOf(h?.description ?: "") }

    var frequency by remember { mutableStateOf(h?.frequency ?: HabitFrequency.DAILY) }
    var weekDaysMask by remember { mutableIntStateOf(h?.weekDaysMask ?: 0) }
    var monthDaysMask by remember { mutableIntStateOf(h?.monthDaysMask ?: 0) }

    var durationAmount by remember {
        val d = h?.durationDays ?: 0
        mutableStateOf(if (d == 0) "" else when (initialDurationUnit) { "Months" -> "${d / 30}"; "Weeks" -> "${d / 7}"; else -> "$d" })
    }
    var durationUnit by remember { mutableStateOf(if (h != null) initialDurationUnit else "Weeks") }

    var colorIndex by remember { mutableIntStateOf(h?.colorIndex ?: 0) }
    var colorArgb by remember { mutableStateOf(h?.colorArgb) }
    var iconIndex by remember { mutableIntStateOf(h?.iconIndex ?: -1) }

    var isChecklist by remember { mutableStateOf(h?.isChecklist ?: false) }
    val checklistItems = remember { mutableStateListOf<String>() }
    // The habit's items load asynchronously, so they arrive after the first composition.
    LaunchedEffect(initialChecklist) {
        if (checklistItems.isEmpty()) checklistItems.addAll(initialChecklist)
    }
    var newChecklistItem by remember { mutableStateOf("") }

    var reminderEnabled by remember { mutableStateOf(initialReminder != null) }
    var reminderHour by remember { mutableIntStateOf(initialReminder?.first ?: 9) }
    var reminderMinute by remember { mutableIntStateOf(initialReminder?.second ?: 0) }
    var untilEndOfYear by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val durationDays = run {
        val amount = durationAmount.toIntOrNull() ?: 0
        when (durationUnit) {
            "Weeks" -> amount * 7
            "Months" -> amount * 30
            else -> amount
        }
    }

    RhythmSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Android's AddHabitSheet layout: title, NAME, DESCRIPTION, then glass panels for
            // Appearance, Reminder, Schedule and Checklist, and the glass primary button.
            EditorialTitle(if (initialHabit != null) "Edit Habit" else "New Habit", modifier = Modifier.padding(bottom = 8.dp))

            FluidTextField(value = name, onValueChange = { name = it }, label = "NAME")
            FluidTextField(
                value = description,
                onValueChange = { description = it },
                label = "DESCRIPTION (OPTIONAL)",
                singleLine = false,
                maxLines = 3
            )

            EditorSection(label = "APPEARANCE") {
                ColorPickerRow(
                    colorIndex = colorIndex,
                    colorArgb = colorArgb,
                    onColorSelected = { idx, argb -> colorIndex = idx; colorArgb = argb },
                    viewModel = koinViewModel()
                )
                IconPickerButton(
                    selectedIconIndex = iconIndex,
                    accentColor = resolveDisplayColor(colorIndex, colorArgb),
                    onIconSelected = { iconIndex = it }
                )
            }

            EditorSection(label = "REMINDER") {
                Surface(
                    onClick = { showTimePicker = true },
                    shape = MaterialTheme.shapes.small,
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().crystalControlSurface()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Alarm, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (reminderEnabled) "%02d:%02d".format(reminderHour, reminderMinute) else "Set Reminder Time",
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (reminderEnabled) {
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Clear",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.clickable { reminderEnabled = false }
                            )
                        }
                    }
                }
            }

            EditorSection(label = "SCHEDULE") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    HabitFrequency.entries.forEachIndexed { index, freq ->
                        SegmentedButton(
                            colors = crystalSegmentedButtonColors(),
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            shape = SegmentedButtonDefaults.itemShape(index, HabitFrequency.entries.size)
                        ) { Text(freq.name.lowercase().replaceFirstChar { it.uppercaseChar() }) }
                    }
                }
                when (frequency) {
                    HabitFrequency.WEEKLY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Repeat on", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        WeekDayPicker(mask = weekDaysMask, onMaskChange = { weekDaysMask = it })
                    }
                    HabitFrequency.MONTHLY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Repeat on days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..31).toList().chunked(6).forEach { rowDays ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                    rowDays.forEach { day ->
                                        val bit = 1 shl (day - 1)
                                        DayCircle(
                                            label = day.toString(),
                                            selected = (monthDaysMask and bit) != 0,
                                            onClick = { monthDaysMask = monthDaysMask xor bit },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(6 - rowDays.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                    HabitFrequency.DAILY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Android: a daily habit with an empty mask repeats every day; unticking this
                        // narrows it to chosen weekdays.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { weekDaysMask = if (weekDaysMask == 0) EVERY_WEEKDAY_MASK else 0 }
                        ) {
                            Checkbox(
                                colors = crystalCheckboxColors(),
                                checked = weekDaysMask == 0,
                                onCheckedChange = { weekDaysMask = if (weekDaysMask == 0) EVERY_WEEKDAY_MASK else 0 }
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Repeats every day", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (weekDaysMask != 0) {
                            Text("Repeat on", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            WeekDayPicker(mask = weekDaysMask, onMaskChange = { weekDaysMask = it })
                        }
                    }
                }
                Text("Goal Duration (optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().alpha(if (untilEndOfYear) 0.38f else 1f)
                ) {
                    FluidTextField(
                        value = durationAmount,
                        onValueChange = { durationAmount = it.filter { c -> c.isDigit() } },
                        label = "AMOUNT",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        DURATION_UNITS.forEachIndexed { index, unit ->
                            SegmentedButton(
                                colors = crystalSegmentedButtonColors(),
                                selected = durationUnit == unit && !untilEndOfYear,
                                onClick = { if (!untilEndOfYear) durationUnit = unit },
                                shape = SegmentedButtonDefaults.itemShape(index, DURATION_UNITS.size)
                            ) { Text(unit) }
                        }
                    }
                }
                FilterChip(
                    colors = crystalFilterChipColors(),
                    selected = untilEndOfYear,
                    onClick = { untilEndOfYear = !untilEndOfYear },
                    label = { Text("Until end of year") },
                    leadingIcon = if (untilEndOfYear) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }

            EditorSection(label = "CHECKLIST (OPTIONAL)") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                        .clickable { isChecklist = !isChecklist; if (!isChecklist) checklistItems.clear() }
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Break this into steps", style = MaterialTheme.typography.bodyLarge)
                        Text("Counts as done once every step is ticked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = isChecklist, onCheckedChange = null, colors = crystalSwitchColors())
                }
                if (isChecklist) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Subtasks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        checklistItems.forEachIndexed { index, label ->
                            FluidTextField(value = label, onValueChange = { checklistItems[index] = it }, label = "ITEM ${index + 1}")
                        }
                        val commitNewItem = {
                            val trimmed = newChecklistItem.trim()
                            if (trimmed.isNotEmpty()) { checklistItems.add(trimmed); newChecklistItem = "" }
                        }
                        FluidTextField(
                            value = newChecklistItem,
                            onValueChange = { newChecklistItem = it },
                            label = "NEW SUBTASK",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { commitNewItem() })
                        )
                        Surface(
                            onClick = commitNewItem,
                            enabled = newChecklistItem.isNotBlank(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                val tint = if (newChecklistItem.isNotBlank()) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                Icon(Icons.Filled.Add, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Add item", style = MaterialTheme.typography.bodyMedium, color = tint)
                            }
                        }
                    }
                }
            }

            MomentumButton(
                text = if (initialHabit != null) "Update Habit" else "Create Habit",
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val finalItems = (if (newChecklistItem.isNotBlank()) checklistItems + newChecklistItem.trim() else checklistItems.toList())
                        .map { it.trim() }.filter { it.isNotEmpty() }
                    val totalDays = if (untilEndOfYear) {
                        val today = LocalDate.now()
                        today.daysUntil(LocalDate(today.year, 12, 31)) + 1
                    } else durationDays
                    onSave(
                        name,
                        description,
                        frequency,
                        weekDaysMask,
                        monthDaysMask,
                        isChecklist,
                        finalItems,
                        colorIndex,
                        colorArgb,
                        totalDays,
                        iconIndex,
                        if (reminderEnabled) "%02d:%02d".format(reminderHour, reminderMinute) else null
                    )
                }
            )
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = reminderHour, initialMinute = reminderMinute, is24Hour = is24Hour)
        RhythmAlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    reminderHour = state.hour
                    reminderMinute = state.minute
                    reminderEnabled = true
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) }
        )
    }
}

