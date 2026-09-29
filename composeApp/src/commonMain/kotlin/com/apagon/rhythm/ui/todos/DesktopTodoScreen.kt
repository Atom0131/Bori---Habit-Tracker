package com.apagon.rhythm.ui.todos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.components.crystalBareTextFieldColors
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalCheckboxColors
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.util.getDueDateAsLocalDate

// Stage 17e: To-dos is no longer a standalone sidebar screen — the real Android app has no
// separate To-dos destination at all; HabitListScreen.kt (the "Today" tab) composes TodoViewModel
// state directly alongside habits in one continuous list. This is now a LazyListScope extension
// appended after the habit sections in DesktopTodayScreen's list mode, instead of its own
// Scaffold+width-cap+LazyColumn screen. DesktopTodoRow (the per-todo card) is unchanged content,
// just no longer wrapped in its own screen shell.
internal fun LazyListScope.todoSection(
    pending: List<Todo>,
    completed: List<Todo>,
    newTitle: String,
    onNewTitleChange: (String) -> Unit,
    onAdd: () -> Unit,
    onToggle: (Todo) -> Unit,
    onDelete: (Todo) -> Unit
) {
    item(key = "todos_header") {
        Text(
            "To-dos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing)
        )
    }
    item(key = "todos_add") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding)
                .crystalCardSurface().padding(DesktopLayout.compactCardPadding),
            horizontalArrangement = Arrangement.spacedBy(DesktopLayout.itemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = onNewTitleChange,
                label = { Text("New to-do") },
                colors = crystalBareTextFieldColors(),
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAdd) { Text("Add") }
        }
    }
    if (pending.isEmpty() && completed.isEmpty()) {
        item(key = "todos_empty") {
            Text(
                "No to-dos yet — add one above.",
                modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing)
            )
        }
    } else {
        items(pending, key = { "todo_${it.id}" }) { todo ->
            DesktopTodoRow(todo, onToggle = { onToggle(todo) }, onDelete = { onDelete(todo) })
        }
        items(completed, key = { "todo_${it.id}" }) { todo ->
            DesktopTodoRow(todo, onToggle = { onToggle(todo) }, onDelete = { onDelete(todo) })
        }
    }
}

@Composable
internal fun DesktopTodoRow(todo: Todo, onToggle: () -> Unit, onDelete: () -> Unit) {
    val dueDate = remember(todo.dueDate) { todo.getDueDateAsLocalDate() }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding, vertical = 4.dp)
            .crystalTileSurface().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = todo.isCompleted, onCheckedChange = { onToggle() }, colors = crystalCheckboxColors())
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
