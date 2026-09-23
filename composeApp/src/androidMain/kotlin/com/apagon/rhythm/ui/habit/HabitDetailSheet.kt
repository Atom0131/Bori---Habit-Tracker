package com.apagon.rhythm.ui.habit

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistProgress
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.ui.util.resolveDisplayColor

@Composable
fun HabitDetailSheet(
    habit: Habit,
    checklistProgress: ChecklistProgress?,
    isDone: Boolean,
    streakDays: Int,
    onDismiss: () -> Unit,
    onToggleItem: (ChecklistItem, Boolean) -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onSetReminder: (String?) -> Unit = {}
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.75f).dp
    val habitColor = resolveDisplayColor(habit.colorIndex, habit.colorArgb)
    val context = LocalContext.current
    var reminderTime by remember(habit.id) { mutableStateOf(habit.reminderTime) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .heightIn(max = maxHeight),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ── Header ──────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(habitColor.copy(alpha = 0.12f))
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 24.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(width = 32.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )

                    // Edit button
                    IconButton(
                        onClick = { onEdit(); onDismiss() },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit habit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Header content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Streak badge
                        if (streakDays > 0) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "🔥",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "$streakDays Day Streak",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        letterSpacing = androidx.compose.ui.unit.TextUnit(
                                            1.5f, androidx.compose.ui.unit.TextUnitType.Sp
                                        )
                                    )
                                }
                            }
                        }

                        // Habit color dot + name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(habitColor)
                            )
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Description
                        if (habit.description.isNotBlank()) {
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Body ────────────────────────────────────────────────────────
                val hasSubtasks = habit.isChecklist &&
                        checklistProgress != null &&
                        checklistProgress.items.isNotEmpty()

                if (hasSubtasks && checklistProgress != null) {
                    val doneCount = checklistProgress.checkedItemIds.size
                    val totalCount = checklistProgress.items.size

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Subtasks",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "$doneCount/$totalCount DONE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = habitColor
                                )
                            }
                        }

                        items(checklistProgress.items, key = { it.id }) { item ->
                            val itemDone = item.id in checklistProgress.checkedItemIds
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = if (itemDone)
                                    habitColor.copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = if (itemDone) null else BorderStroke(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleItem(item, itemDone) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .then(
                                            if (itemDone)
                                                Modifier.background(
                                                    color = Color.Transparent,
                                                    shape = MaterialTheme.shapes.large
                                                )
                                            else Modifier
                                        )
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    // Left accent bar for done items
                                    if (itemDone) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .size(width = 4.dp, height = 24.dp)
                                                .clip(CircleShape)
                                                .background(habitColor)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                    }

                                    Icon(
                                        imageVector = if (itemDone) Icons.Default.CheckBox
                                        else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (itemDone) habitColor
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (itemDone) FontWeight.SemiBold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Footer ──────────────────────────────────────────────────────
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Reminder row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = if (reminderTime != null) habitColor
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (reminderTime != null) "Reminder: $reminderTime"
                                   else "Daily Reminder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (reminderTime != null) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        if (reminderTime != null) {
                            TextButton(onClick = {
                                reminderTime = null
                                onSetReminder(null)
                            }) { Text("Clear") }
                        }
                        TextButton(onClick = {
                            val parts = reminderTime?.split(":")
                            val initHour = parts?.getOrNull(0)?.toIntOrNull()
                                ?: java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                            val initMin = parts?.getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(context, { _, hour, minute ->
                                val t = "%02d:%02d".format(hour, minute)
                                reminderTime = t
                                onSetReminder(t)
                            }, initHour, initMin, false).show()
                        }) { Text(if (reminderTime != null) "Change" else "Set Time") }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedContent(
                        targetState = isDone,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        modifier = Modifier.weight(1f),
                        label = "doneButtonState"
                    ) { done ->
                    if (done) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckBox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "COMPLETED",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "COMPLETE HABIT",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                    } // end AnimatedContent
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }
    }
}
