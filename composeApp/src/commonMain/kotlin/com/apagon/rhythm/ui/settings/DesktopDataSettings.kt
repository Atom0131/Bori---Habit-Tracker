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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.ui.components.CrystalWindowContent
import com.apagon.rhythm.ui.components.crystalCardSurface

// Desktop counterpart to androidMain's DataSettings.kt (Stage 11). Swaps
// IconButton(Icons.Default.Restore) for a plain text "Unarchive" TextButton,
// same no-Material-Icons-on-desktop convention DesktopTodoScreen.kt (Stage 7)
// established for its "Delete" button.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopArchivedHabitsSheet(
    habits: List<Habit>,
    onDismiss: () -> Unit,
    onUnarchive: (Habit) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        CrystalWindowContent {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Archived Habits", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

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
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { onUnarchive(habit) }) { Text("Unarchive") }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopArchivedTodosSheet(
    todos: List<Todo>,
    onDismiss: () -> Unit,
    onUnarchive: (Todo) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        CrystalWindowContent {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Archived To-dos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

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
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(todo.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (todo.dueDate.isNotEmpty()) {
                                        Text(
                                            "Due: ${todo.dueDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(onClick = { onUnarchive(todo) }) { Text("Unarchive") }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
        }
    }
}
