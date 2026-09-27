package com.apagon.rhythm.ui.todos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.util.getDueDateAsLocalDate
import org.koin.compose.viewmodel.koinViewModel

// Stage 7's Todo screen, in the same deliberately-narrow spirit as
// DesktopHabitScreen.kt (Stage 3): plain Material3, title + due-date only
// (no priority/icon/sound picker yet — those need the desktop sound
// substitute and a date-picker dialog neither of which exist yet), reusing
// the already-common TodoViewModel wholesale rather than porting Android's
// TodoRowComposable/AddTodoSheet UI polish (SwipeToDeleteBox, HabitCard,
// priority pills), which depend on ui/util/SharedComposables.kt — not yet
// in commonMain.
@Composable
fun DesktopTodoScreen(viewModel: TodoViewModel = koinViewModel()) {
    val pending by viewModel.pendingTodos.collectAsState()
    val completed by viewModel.completedTodos.collectAsState()
    var newTitle by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().crystalCardSurface().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("New to-do") },
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                if (newTitle.isNotBlank()) {
                    viewModel.addTodo(
                        title = newTitle,
                        note = "",
                        dueDate = "",
                        priority = com.apagon.rhythm.data.model.TodoPriority.NONE
                    )
                    newTitle = ""
                }
            }) {
                Text("Add")
            }
        }

        if (pending.isEmpty() && completed.isEmpty()) {
            Text(
                "No to-dos yet — add one above.",
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                items(pending, key = { it.id }) { todo ->
                    DesktopTodoRow(todo, onToggle = { viewModel.toggleCompletion(todo) }, onDelete = { viewModel.deleteTodo(todo) })
                }
                items(completed, key = { it.id }) { todo ->
                    DesktopTodoRow(todo, onToggle = { viewModel.toggleCompletion(todo) }, onDelete = { viewModel.deleteTodo(todo) })
                }
            }
        }
    }
}

@Composable
private fun DesktopTodoRow(todo: Todo, onToggle: () -> Unit, onDelete: () -> Unit) {
    val dueDate = remember(todo.dueDate) { todo.getDueDateAsLocalDate() }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .crystalTileSurface().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = todo.isCompleted, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                todo.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
            if (todo.dueDate.isNotEmpty() && dueDate != null) {
                Text(
                    "Due $dueDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onDelete) { Text("Delete") }
    }
}
