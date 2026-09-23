package com.apagon.rhythm.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.util.EditorialTitle
import com.apagon.rhythm.ui.util.HabitCard
import com.apagon.rhythm.ui.util.MomentumButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedHabitsSheet(
    habits: List<com.apagon.rhythm.data.model.Habit>,
    onDismiss: () -> Unit,
    onUnarchive: (com.apagon.rhythm.data.model.Habit) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            EditorialTitle("Archived Habits")

            Spacer(Modifier.height(8.dp))

            if (habits.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No archived habits found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(habits.size) { index ->
                        val habit = habits[index]
                        HabitCard {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onUnarchive(habit) }) {
                                    Icon(Icons.Default.Restore, contentDescription = "Unarchive", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            MomentumButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedTodosSheet(
    todos: List<com.apagon.rhythm.data.model.Todo>,
    onDismiss: () -> Unit,
    onUnarchive: (com.apagon.rhythm.data.model.Todo) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            EditorialTitle("Archived To-dos")

            Spacer(Modifier.height(8.dp))

            if (todos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No archived to-dos found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(todos.size) { index ->
                        val todo = todos[index]
                        HabitCard {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(todo.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (todo.dueDate.isNotEmpty()) {
                                        Text("Due: ${todo.dueDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { onUnarchive(todo) }) {
                                    Icon(Icons.Default.Restore, contentDescription = "Unarchive", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            MomentumButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
