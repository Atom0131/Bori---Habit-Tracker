package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
import com.apagon.rhythm.ui.theme.habitColorPalette
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import com.apagon.rhythm.ui.util.RhythmSheet
import com.apagon.rhythm.ui.util.habitIconLibrary
import org.koin.compose.koinInject

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val DURATION_UNITS = listOf("Days", "Weeks", "Months")

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

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var frequency by remember { mutableStateOf(HabitFrequency.DAILY) }
    var weekDaysMask by remember { mutableIntStateOf(0) }
    var monthDaysMask by remember { mutableIntStateOf(0) }

    var durationAmount by remember { mutableStateOf("") }
    var durationUnit by remember { mutableStateOf("Weeks") }

    var colorIndex by remember { mutableIntStateOf(0) }
    var iconIndex by remember { mutableIntStateOf(-1) }

    var isChecklist by remember { mutableStateOf(false) }
    val checklistItems = remember { mutableStateListOf<String>() }
    var newChecklistItem by remember { mutableStateOf("") }

    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableIntStateOf(9) }
    var reminderMinute by remember { mutableIntStateOf(0) }
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
            Text("New Habit", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                colors = crystalTextFieldColors(),
                shape = crystalTextFieldShape(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                colors = crystalTextFieldColors(),
                shape = crystalTextFieldShape(),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Schedule ──────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SCHEDULE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitFrequency.entries.forEach { freq ->
                        ChoiceChip(
                            label = freq.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = frequency == freq,
                            onClick = { frequency = freq }
                        )
                    }
                }

                if (frequency == HabitFrequency.WEEKLY) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DAY_LABELS.forEachIndexed { index, day ->
                            val bit = 1 shl index
                            DayChip(
                                label = day.take(1),
                                selected = (weekDaysMask and bit) != 0,
                                onClick = { weekDaysMask = weekDaysMask xor bit }
                            )
                        }
                    }
                }

                if (frequency == HabitFrequency.MONTHLY) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0 until 31 step 7).forEach { rowStart ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (day in rowStart until minOf(rowStart + 7, 31)) {
                                    val bit = 1 shl day
                                    DayChip(
                                        label = (day + 1).toString(),
                                        selected = (monthDaysMask and bit) != 0,
                                        onClick = { monthDaysMask = monthDaysMask xor bit }
                                    )
                                }
                            }
                        }
                    }
                }

                Text("Goal duration (optional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = durationAmount,
                        onValueChange = { durationAmount = it.filter { c -> c.isDigit() } },
                        label = { Text("Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = crystalTextFieldColors(),
                        shape = crystalTextFieldShape(),
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DURATION_UNITS.forEach { unit ->
                            ChoiceChip(label = unit, selected = durationUnit == unit, onClick = { durationUnit = unit })
                        }
                    }
                }
            }

            // ── Appearance ────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("APPEARANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    habitColorPalette.forEachIndexed { index, color ->
                        val isSelected = colorIndex == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 44.dp else 40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorIndex = index }
                        )
                    }
                }
                HabitIconPickerRow(selectedIndex = iconIndex, onSelect = { iconIndex = it })
            }

            // ── Checklist ─────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Checklist habit", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = isChecklist, onCheckedChange = { isChecklist = it }, colors = crystalSwitchColors())
                }
                if (isChecklist) {
                    checklistItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = item,
                                onValueChange = { checklistItems[index] = it },
                                singleLine = true,
                                colors = crystalTextFieldColors(),
                                shape = crystalTextFieldShape(),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { checklistItems.removeAt(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove step")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newChecklistItem,
                            onValueChange = { newChecklistItem = it },
                            label = { Text("Add step") },
                            singleLine = true,
                            colors = crystalTextFieldColors(),
                            shape = crystalTextFieldShape(),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (newChecklistItem.isNotBlank()) {
                                checklistItems.add(newChecklistItem.trim())
                                newChecklistItem = ""
                            }
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add step")
                        }
                    }
                }
            }

            // ── Reminder ──────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Reminder", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it }, colors = crystalSwitchColors())
                }
                if (reminderEnabled) {
                    Row(
                        modifier = Modifier
                            .crystalControlSurface(shape = MaterialTheme.shapes.large)
                            .clickable(onClick = { showTimePicker = true }),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("%02d:%02d".format(reminderHour, reminderMinute), modifier = Modifier.padding(16.dp))
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        name,
                        description,
                        frequency,
                        weekDaysMask,
                        monthDaysMask,
                        isChecklist,
                        checklistItems.toList(),
                        colorIndex,
                        null,
                        durationDays,
                        iconIndex,
                        if (reminderEnabled) "%02d:%02d".format(reminderHour, reminderMinute) else null
                    )
                },
                enabled = name.isNotBlank(),
                colors = crystalButtonColors(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create Habit") }
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
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) }
        )
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .crystalChipSurface(
                fill = if (selected) crystalSelectedChipColor(MaterialTheme.colorScheme.primary)
                else MaterialTheme.colorScheme.surfaceContainerHighest
            )
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) crystalSelectedChipContentColor(MaterialTheme.colorScheme.onPrimary) else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .crystalChipSurface(
                fill = if (selected) crystalSelectedChipColor(MaterialTheme.colorScheme.primary)
                else MaterialTheme.colorScheme.surfaceContainerHighest
            )
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp).widthIn(min = 24.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) crystalSelectedChipContentColor(MaterialTheme.colorScheme.onPrimary) else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun HabitIconPickerRow(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Icon (optional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(habitIconLibrary.size) { index ->
                val (label, icon) = habitIconLibrary[index]
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .crystalChipSurface(
                            fill = if (isSelected) crystalSelectedChipColor(MaterialTheme.colorScheme.primary)
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        .clickable { onSelect(if (isSelected) -1 else index) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (isSelected) crystalSelectedChipContentColor(MaterialTheme.colorScheme.onPrimary) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
