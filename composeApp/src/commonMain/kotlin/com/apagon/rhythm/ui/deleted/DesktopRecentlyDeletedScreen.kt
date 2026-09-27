package com.apagon.rhythm.ui.deleted

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.ui.components.crystalTileSurface
import org.koin.compose.viewmodel.koinViewModel

// Stage 7's Recently Deleted screen — see DesktopRecentlyDeletedViewModel's
// doc comment for why this covers Habit+Todo only, not the full 9-way split
// Android's RecentlyDeletedScreen.kt shows. Grows a section at a time as
// later stages add each entity to DesktopHabitDatabase.
@Composable
fun DesktopRecentlyDeletedScreen(viewModel: DesktopRecentlyDeletedViewModel = koinViewModel()) {
    val items by viewModel.deletedItems.collectAsState()

    if (items.isEmpty) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Your trash is empty", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Items here will be permanently deleted after 14 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (items.habits.isNotEmpty()) {
            item { DeletedSectionHeader("Habits") }
            items(items.habits, key = { "habit-${it.id}" }) { habit: Habit ->
                DesktopDeletedItemRow(
                    title = habit.name,
                    onRestore = { viewModel.restoreHabit(habit) },
                    onDelete = { viewModel.hardDeleteHabit(habit) }
                )
            }
        }
        if (items.todos.isNotEmpty()) {
            item { DeletedSectionHeader("To-Dos") }
            items(items.todos, key = { "todo-${it.id}" }) { todo: Todo ->
                DesktopDeletedItemRow(
                    title = todo.title,
                    onRestore = { viewModel.restoreTodo(todo) },
                    onDelete = { viewModel.hardDeleteTodo(todo) }
                )
            }
        }
    }
}

@Composable
private fun DeletedSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun DesktopDeletedItemRow(title: String, onRestore: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().crystalTileSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onRestore) { Text("Restore") }
        TextButton(onClick = onDelete) { Text("Delete") }
    }
}
