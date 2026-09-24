package com.apagon.rhythm.ui.habit
import com.apagon.rhythm.core.time.*

import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.EaseInOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.app.TimePickerDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.ui.util.*
import com.apagon.rhythm.core.time.ChronoUnit
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.ui.util.ColorPickerRow
import com.apagon.rhythm.ui.util.IconPickerButton
import com.apagon.rhythm.ui.util.sheetTextFieldColors

import android.media.RingtoneManager
import com.apagon.rhythm.ui.theme.resolveDisplayColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHabitSheet(
    initialHabit: Habit? = null,
    initialItems: List<ChecklistItem> = emptyList(),
    isPro: Boolean = true,
    onDismiss: () -> Unit,
    onShowPaywall: (String) -> Unit = {},
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
        reminderTime: String?,
        soundUri: String,
        vibrationPatternId: String
    ) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val is24Hour = remember { context.isSystem24Hour() }
    val tutorialViewModel: TutorialViewModel = koinViewModel()
    val hasSeenHabitCreationTutorial by tutorialViewModel.hasSeenHabitCreationTutorial.collectAsState(initial = true)
    val hasSeenChecklistHabitTutorial by tutorialViewModel.hasSeenChecklistHabitTutorial.collectAsState(initial = true)
    var name by remember { mutableStateOf(initialHabit?.name ?: "") }
    var description by remember { mutableStateOf(initialHabit?.description ?: "") }
    var frequency by remember { mutableStateOf(initialHabit?.frequency ?: HabitFrequency.DAILY) }
    var weekDaysMask by remember { mutableIntStateOf(initialHabit?.weekDaysMask ?: 0) }
    var monthDaysMask by remember { mutableIntStateOf(initialHabit?.monthDaysMask ?: 0) }
    var isChecklist by remember { mutableStateOf(initialHabit?.isChecklist ?: false) }
    var colorIndex by remember { mutableIntStateOf(initialHabit?.colorIndex ?: 0) }
    var colorArgb by remember { mutableStateOf(initialHabit?.colorArgb) }
    var selectedIconIndex by remember { mutableIntStateOf(initialHabit?.iconIndex ?: -1) }
    var reminderTime by remember { mutableStateOf(initialHabit?.reminderTime) }
    var selectedSoundUri by remember { mutableStateOf(initialHabit?.soundUri ?: "") }
    var selectedPatternId by remember { mutableStateOf(initialHabit?.vibrationPatternId ?: "default") }
    var checklistItemLabels by remember(initialItems) {
        mutableStateOf(initialItems.map { it.label })
    }
    var newItemText by remember { mutableStateOf("") }

    val durationUnits = listOf("Days", "Weeks", "Months")
    val initialDurationUnit = remember(initialHabit?.durationDays) {
        val d = initialHabit?.durationDays ?: 0
        when {
            d > 0 && d % 30 == 0 -> "Months"
            d > 0 && d % 7 == 0  -> "Weeks"
            else -> "Days"
        }
    }
    val initialDurationAmount = remember(initialHabit?.durationDays) {
        val d = initialHabit?.durationDays ?: 0
        if (d == 0) "" else when (initialDurationUnit) {
            "Months" -> (d / 30).toString()
            "Weeks"  -> (d / 7).toString()
            else     -> d.toString()
        }
    }
    var durationAmount by remember { mutableStateOf(initialDurationAmount) }
    var durationUnit by remember { mutableStateOf(initialDurationUnit) }
    var untilEndOfYear by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val weekDayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .blockSheetBoundaryOverscroll()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            EditorialTitle(
                text = if (initialHabit != null) "Edit Habit" else "New Habit",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (initialHabit == null && !hasSeenHabitCreationTutorial) {
                TutorialCard(
                    title = "Creating a habit",
                    description = "A few things to know before you start:",
                    bullets = listOf(
                        "Give it a name — the rest is optional",
                        "Pick how often: daily, specific weekdays, or specific days of the month",
                        "Add a reminder if you want a nudge at a set time",
                        "Turn on \"Checklist habit\" if it's really a set of sub-steps, not one action"
                    ),
                    onDismiss = { tutorialViewModel.setHasSeenHabitCreationTutorial(true) }
                )
            }

            FluidTextField(
                value = name,
                onValueChange = { name = it },
                label = "NAME",
                singleLine = true
            )

            FluidTextField(
                value = description,
                onValueChange = { description = it },
                label = "DESCRIPTION (OPTIONAL)",
                singleLine = false,
                maxLines = 3
            )

            // Color picker
            Text("Card Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // Icon picker
            Text("Icon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconPickerButton(
                selectedIconIndex = selectedIconIndex,
                accentColor = resolveDisplayColor(colorIndex, colorArgb),
                onIconSelected = { selectedIconIndex = it }
            )

            // Daily reminder section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DAILY REMINDER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    onClick = {
                        val parts = reminderTime?.split(":")
                        val initHour = parts?.getOrNull(0)?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                        val initMin = parts?.getOrNull(1)?.toIntOrNull() ?: 0
                        TimePickerDialog(context, { _, hour, minute ->
                            reminderTime = "%02d:%02d".format(hour, minute)
                        }, initHour, initMin, is24Hour).show()
                    },
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
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Alarm, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = reminderTime ?: "Set Reminder Time",
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (reminderTime != null) {
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Clear",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.clickable { reminderTime = null }
                            )
                        }
                    }
                }
            }

            if (reminderTime != null) {
                SoundPickerButton(
                    soundUri = selectedSoundUri,
                    ringtoneType = RingtoneManager.TYPE_NOTIFICATION,
                    onSoundSelected = { selectedSoundUri = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (reminderTime != null) {
                VibrationPatternButton(
                    patternId = selectedPatternId,
                    onPatternSelected = { selectedPatternId = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text("Frequency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                HabitFrequency.entries.forEachIndexed { index, freq ->
                    SegmentedButton(
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        shape = SegmentedButtonDefaults.itemShape(index, HabitFrequency.entries.size)
                    ) {
                        Text(freq.name.lowercase().replaceFirstChar { it.uppercaseChar() })
                    }
                }
            }

            AnimatedContent(
                targetState = frequency,
                transitionSpec = { fadeIn(tween(250, easing = EaseInOut)) togetherWith fadeOut(tween(200, easing = EaseInOut)) },
                label = "frequencyContent"
            ) { freq ->
            when (freq) {
                HabitFrequency.WEEKLY -> {
                    Text("Repeat on", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        weekDayLabels.forEachIndexed { index, label ->
                            val selected = (weekDaysMask and (1 shl index)) != 0
                            Surface(
                                onClick = {
                                    weekDaysMask = if (selected)
                                        weekDaysMask and (1 shl index).inv()
                                    else
                                        weekDaysMask or (1 shl index)
                                },
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                                shadowElevation = if (selected) 4.dp else 2.dp,
                                tonalElevation = if (selected) 2.dp else 1.dp,
                                border = if (selected) null else BorderStroke(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                HabitFrequency.MONTHLY -> {
                    Text("Repeat on days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..31).toList().chunked(6).forEach { rowDays ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowDays.forEach { day ->
                                    val selected = (monthDaysMask and (1 shl (day - 1))) != 0
                                    Surface(
                                        onClick = {
                                            monthDaysMask = if (selected)
                                                monthDaysMask and (1 shl (day - 1)).inv()
                                            else
                                                monthDaysMask or (1 shl (day - 1))
                                        },
                                        shape = CircleShape,
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        shadowElevation = if (selected) 4.dp else 2.dp,
                                        tonalElevation = if (selected) 2.dp else 1.dp,
                                        border = if (selected) null else BorderStroke(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = day.toString(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                repeat(6 - rowDays.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                HabitFrequency.DAILY -> Unit
            }
            } // end AnimatedContent

            // Goal duration
            Text("Goal Duration (optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (untilEndOfYear) 0.38f else 1f)
            ) {
                FluidTextField(
                    value = durationAmount,
                    onValueChange = { durationAmount = it.filter { c -> c.isDigit() } },
                    label = "AMOUNT",
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    durationUnits.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = durationUnit == unit && !untilEndOfYear,
                            onClick = { if (!untilEndOfYear) durationUnit = unit },
                            shape = SegmentedButtonDefaults.itemShape(index, durationUnits.size)
                        ) {
                            Text(unit)
                        }
                    }
                }
            }
            FilterChip(
                selected = untilEndOfYear,
                onClick = { untilEndOfYear = !untilEndOfYear },
                label = { Text("Until end of year") },
                leadingIcon = if (untilEndOfYear) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )

            // Checklist toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Checklist habit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isChecklist,
                    onCheckedChange = { checked ->
                        isChecklist = checked
                        if (!checked) checklistItemLabels = emptyList()
                    }
                )
            }

            if (isChecklist && !hasSeenChecklistHabitTutorial) {
                TutorialCard(
                    title = "Checklist habits",
                    description = "Want to know how these work?",
                    bullets = listOf(
                        "Add each sub-step as its own item below",
                        "The habit counts as done once every item is checked off",
                        "Great for routines like \"morning stretch\" (warm up, stretch, cool down)"
                    ),
                    onDismiss = { tutorialViewModel.setHasSeenChecklistHabitTutorial(true) }
                )
            }

            // Checklist item editor
            AnimatedVisibility(
                visible = isChecklist,
                enter = expandVertically(
                    // tween, not spring — same fix as WeekStripComposable.kt's calendar expand;
                    // matches the exit below instead of a slow overshoot-prone spring.
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(200)),
                exit = shrinkVertically(
                    animationSpec = tween(250, easing = FastOutLinearInEasing)
                ) + fadeOut(tween(150))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Subtasks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    checklistItemLabels.forEachIndexed { index, label ->
                        FluidTextField(
                            value = label,
                            onValueChange = { newLabel ->
                                checklistItemLabels = checklistItemLabels.toMutableList()
                                    .also { it[index] = newLabel }
                            },
                            label = "ITEM ${index + 1}",
                            singleLine = true
                        )
                    }

                    FluidTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        label = "NEW SUBTASK",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            MomentumButton(
                text = if (initialHabit != null) "Update Habit" else "Create Habit",
                onClick = {
                    if (reminderTime != null && !hasTechnicalPermissions(context)) {
                        showPermissionDialog = true
                        return@MomentumButton
                    }
                    val finalItems = if (newItemText.isNotBlank())
                        checklistItemLabels + newItemText.trim()
                    else
                        checklistItemLabels
                    val totalDays = when {
                        untilEndOfYear -> {
                            val today = LocalDate.now()
                            ChronoUnit.DAYS.between(today, LocalDate.of(today.year, 12, 31)).toInt() + 1
                        }
                        durationUnit == "Weeks"  -> (durationAmount.toIntOrNull() ?: 0) * 7
                        durationUnit == "Months" -> (durationAmount.toIntOrNull() ?: 0) * 30
                        else                     -> durationAmount.toIntOrNull() ?: 0
                    }
                    onSave(
                        name, description, frequency, weekDaysMask, monthDaysMask,
                        isChecklist, finalItems, colorIndex, colorArgb, totalDays,
                        selectedIconIndex, reminderTime, selectedSoundUri, selectedPatternId
                    )
                },
                enabled = name.isNotBlank()
                    && when (frequency) {
                        HabitFrequency.WEEKLY -> weekDaysMask != 0
                        HabitFrequency.MONTHLY -> monthDaysMask != 0
                        HabitFrequency.DAILY -> true
                    }
                    && (!isChecklist || checklistItemLabels.isNotEmpty() || newItemText.isNotBlank()),
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
}
