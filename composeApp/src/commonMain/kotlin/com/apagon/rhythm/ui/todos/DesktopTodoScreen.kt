package com.apagon.rhythm.ui.todos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.components.crystalCheckboxColors
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.util.CollapsibleSectionHeader
import com.apagon.rhythm.ui.util.SectionHeaderTier
import com.apagon.rhythm.ui.util.getDueDateAsLocalDate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import com.apagon.rhythm.ui.util.RoundCheck

// Stage 17e: To-dos is no longer a standalone sidebar screen — the real Android app has no
// separate To-dos destination at all; HabitListScreen.kt (the "Today" tab) composes TodoViewModel
// state directly alongside habits in one continuous list. This is now a LazyListScope extension
// appended after the habit sections in DesktopTodayScreen's list mode, instead of its own
// Scaffold+width-cap+LazyColumn screen. DesktopTodoRow (the per-todo card) is unchanged content,
// just no longer wrapped in its own screen shell.
// Stage 18: Android's real HabitListScreen.kt also renders its "todos" section through
// CollapsibleSectionHeader, using the exact same collapsedSections set the Daily/Weekly/Monthly
// habit sections use — everything starts folded closed, not just habits. Matches that here with
// its own expanded/onToggleExpanded pair (kept separate from the habit frequencies' Set<HabitFrequency>
// state rather than widening that type, since To-dos isn't a HabitFrequency).
// Stage 19e: the inline "New to-do" row used to sit here — removed now that the Today FAB
// (Stage 19c) is the single discoverable add entry point for both habits and to-dos, matching
// Android's HabitListScreen.kt (no inline todosSection add row there either, only the FAB).
internal fun LazyListScope.todoSection(
    pending: List<Todo>,
    completed: List<Todo>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggle: (Todo) -> Unit,
    onDelete: (Todo) -> Unit
) {
    item(key = "todos_header") {
        CollapsibleSectionHeader(
            title = "To-dos",
            expanded = expanded,
            onToggle = onToggleExpanded,
            tier = SectionHeaderTier.Primary
        )
    }
    if (!expanded) return

    if (pending.isEmpty() && completed.isEmpty()) {
        item(key = "todos_empty") {
            Text(
                "No to-dos yet — tap + to add one.",
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
        RoundCheck(checked = todo.isCompleted, onToggle = onToggle)
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
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
