package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistProgress
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.ui.util.HabitCard
import com.apagon.rhythm.ui.util.SwipeToDeleteBox
import com.apagon.rhythm.ui.util.resolveDisplayColor
import com.apagon.rhythm.ui.util.ConfettiAnimation
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun HabitRow(
    habit: Habit,
    isDone: Boolean,
    onToggle: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onView: () -> Unit,
    modifier: Modifier = Modifier,
    isDueToday: Boolean = false,
    isDueTomorrow: Boolean = false,
    checklistProgress: ChecklistProgress? = null,
    onToggleItem: (ChecklistItem, Boolean) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    var localIsDone by remember(isDone) { mutableStateOf(isDone) }

    SwipeToDeleteBox(
        onDelete = onDelete,
        onArchive = onArchive,
        deleteContentDescription = "Delete habit",
        archiveContentDescription = "Archive habit",
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        HabitCard(isDone = localIsDone, habitColor = resolveDisplayColor(habit.colorIndex, habit.colorArgb), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Colored dot indicator in top-right corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(10.dp)
                        .background(
                            color = if (localIsDone)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            else
                                resolveDisplayColor(habit.colorIndex, habit.colorArgb),
                            shape = CircleShape
                        )
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = onView, onLongClick = onEdit)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val context = LocalContext.current
                        
                        // Leading: toggle button
                        IconButton(onClick = {
                            if (!habit.isChecklist || localIsDone) {
                                scope.launch {
                                    val newDone = !localIsDone
                                    localIsDone = newDone
                                    
                                    if (newDone) {
                                        // Haptic feedback
                                        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                        } else {
                                            vibrator?.vibrate(50)
                                        }
                                    }

                                    delay(400)
                                    onToggle()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (localIsDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (localIsDone) "Completed" else "Not completed",
                                tint = if (localIsDone) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Confetti Layer
                        var showConfetti by remember { mutableStateOf(false) }
                        if (showConfetti) {
                            ConfettiAnimation(
                                modifier = Modifier.fillMaxSize(),
                                onAnimationEnd = { showConfetti = false }
                            )
                        }

                        LaunchedEffect(localIsDone) {
                            if (localIsDone && !isDone) { // Only on first manual check
                                showConfetti = true
                            }
                        }
                        // Habit name + edit icon + badges (adaptive layout)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    habit.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (localIsDone) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (localIsDone) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = onEdit, modifier = Modifier.size(20.dp)) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit habit",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            val badgeLabel = when {
                                isDueToday && !localIsDone -> "Due Today"
                                isDueTomorrow && !localIsDone -> "Due Tomorrow"
                                else -> null
                            }

                            if (badgeLabel != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (badgeLabel == "Due Today") MaterialTheme.colorScheme.primary 
                                                else resolveDisplayColor(habit.colorIndex, habit.colorArgb), 
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (badgeLabel == "Due Today") MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
