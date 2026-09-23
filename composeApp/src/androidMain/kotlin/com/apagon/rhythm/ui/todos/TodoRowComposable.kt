package com.apagon.rhythm.ui.todos
import com.apagon.rhythm.core.time.*

import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.apagon.rhythm.ui.util.HabitCard
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.TodoPriority
import com.apagon.rhythm.ui.util.SwipeToDeleteBox
import com.apagon.rhythm.ui.util.todoIconList
import com.apagon.rhythm.ui.util.todoPriorityColors
import android.text.format.DateFormat
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodoRow(
    todo: Todo,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var localIsCompleted by remember(todo.isCompleted) { mutableStateOf(todo.isCompleted) }
    
    val priority = TodoPriority.entries.firstOrNull { it.name == todo.priority } ?: TodoPriority.NONE
    val today = remember { LocalDate.now() }
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    
    val dueDate = remember(todo.dueDate) {
        todo.dueDate.takeIf { it.isNotEmpty() }?.let {
            runCatching { LocalDate.parse(it.substringBefore(" ")) }.getOrNull()
        }
    }
    val dueTime = remember(todo.dueDate, is24Hour) {
        todo.dueDate.takeIf { it.contains(" ") }?.let {
            runCatching { 
                val timePart = it.substringAfter(" ")
                val h = timePart.substringBefore(":").toInt()
                val m = timePart.substringAfter(":").toInt()
                com.apagon.rhythm.ui.util.formatTime(h, m, is24Hour)
            }.getOrNull()
        }
    }
    val isOverdue = dueDate != null && dueDate.isBefore(today) && !todo.isCompleted

    SwipeToDeleteBox(
        onDelete = onDelete,
        onArchive = null,
        deleteContentDescription = "Delete to-do",
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        HabitCard(
            isDone = localIsCompleted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = {
                    scope.launch {
                        localIsCompleted = !localIsCompleted
                        delay(400)
                        onToggle()
                    }
                }) {
                    Icon(
                        imageVector = if (localIsCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (localIsCompleted) "Completed" else "Not completed",
                        tint = if (localIsCompleted) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (localIsCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (localIsCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (todo.note.isNotBlank()) {
                        Text(
                            text = todo.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (dueDate != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val dateText = remember(dueDate, today, dueTime) {
                            val base = when {
                                dueDate == today -> "Due Today"
                                dueDate == today.plusDays(1) -> "Due Tomorrow"
                                isOverdue -> "Overdue ${dueDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
                                else -> "Due ${dueDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
                            }
                            if (dueTime != null) "$base at $dueTime" else base
                        }
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isOverdue -> MaterialTheme.colorScheme.error
                                dueDate == today || dueDate == today.plusDays(1) -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                if (priority != TodoPriority.NONE) {
                    val pillColor = todoPriorityColors[priority] ?: MaterialTheme.colorScheme.surfaceContainerHighest
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = pillColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = priority.name.lowercase().capitalize(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = pillColor
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit to-do",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
