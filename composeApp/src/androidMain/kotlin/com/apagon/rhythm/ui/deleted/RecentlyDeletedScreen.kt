package com.apagon.rhythm.ui.deleted
import com.apagon.rhythm.core.time.*

import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apagon.rhythm.ui.util.EditorialTitle
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import com.apagon.rhythm.core.time.ZoneId
import com.apagon.rhythm.core.time.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyDeletedScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecentlyDeletedViewModel = koinViewModel()
) {
    val items by viewModel.deletedItems.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently Deleted") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (items.isEmpty) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Your trash is empty", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    items(items.habits) { habit ->
                        DeletedItemRow(
                            title = habit.name,
                            deletedAt = habit.deletedAt ?: 0L,
                            onRestore = { viewModel.restoreHabit(habit) },
                            onDelete = { viewModel.hardDeleteHabit(habit) }
                        )
                    }
                }

                if (items.todos.isNotEmpty()) {
                    item { DeletedSectionHeader("To-Dos") }
                    items(items.todos) { todo ->
                        DeletedItemRow(
                            title = todo.title,
                            deletedAt = todo.deletedAt ?: 0L,
                            onRestore = { viewModel.restoreTodo(todo) },
                            onDelete = { viewModel.hardDeleteTodo(todo) }
                        )
                    }
                }

                if (items.reminders.isNotEmpty()) {
                    item { DeletedSectionHeader("Reminders") }
                    items(items.reminders) { reminder ->
                        DeletedItemRow(
                            title = reminder.title,
                            deletedAt = reminder.deletedAt ?: 0L,
                            onRestore = { viewModel.restoreReminder(reminder) },
                            onDelete = { viewModel.hardDeleteReminder(reminder) }
                        )
                    }
                }

                if (items.events.isNotEmpty()) {
                    item { DeletedSectionHeader("Events") }
                    items(items.events) { event ->
                        DeletedItemRow(
                            title = event.title,
                            deletedAt = event.deletedAt ?: 0L,
                            onRestore = { viewModel.restoreEvent(event) },
                            onDelete = { viewModel.hardDeleteEvent(event) }
                        )
                    }
                }

                if (items.alarms.isNotEmpty()) {
                    item { DeletedSectionHeader("Alarms") }
                    items(items.alarms) { alarm ->
                        DeletedItemRow(
                            title = "Alarm ${alarm.hour}:${alarm.minute.toString().padStart(2, '0')}",
                            deletedAt = alarm.deletedAt ?: 0L,
                            onRestore = { viewModel.restoreAlarm(alarm) },
                            onDelete = { viewModel.hardDeleteAlarm(alarm) }
                        )
                    }
                }

                if (items.timers.isNotEmpty()) {
                    item { DeletedSectionHeader("Timers") }
                    items(items.timers) { timer ->
                        DeletedItemRow(
                            title = if (timer.label.isNotBlank()) timer.label else "Timer",
                            deletedAt = timer.deletedAt ?: 0L,
                            onRestore = { viewModel.restoreTimer(timer) },
                            onDelete = { viewModel.hardDeleteTimer(timer) }
                        )
                    }
                }

                if (items.notebooks.isNotEmpty()) {
                    item { DeletedSectionHeader("Notebooks") }
                    items(items.notebooks) { notebook ->
                        DeletedItemRow(
                            title = notebook.name,
                            deletedAt = notebook.deletedAt ?: 0L,
                            onRestore = { viewModel.restoreNotebook(notebook) },
                            onDelete = { viewModel.hardDeleteNotebook(notebook) }
                        )
                    }
                }

                if (items.notes.isNotEmpty()) {
                    item { DeletedSectionHeader("Notes") }
                    items(items.notes) { note ->
                        DeletedItemRow(
                            title = note.title,
                            deletedAt = note.deletedAt ?: 0L,
                            onRestore = { viewModel.restoreNote(note) },
                            onDelete = { viewModel.hardDeleteNote(note) }
                        )
                    }
                }

                if (items.journalEntries.isNotEmpty()) {
                    item { DeletedSectionHeader("Journal Entries") }
                    items(items.journalEntries) { entry ->
                        DeletedItemRow(
                            title = if (entry.title.isNotBlank()) entry.title else "Entry on ${entry.date}",
                            deletedAt = entry.deletedAt ?: 0L,
                            onRestore = { viewModel.restoreJournalEntry(entry) },
                            onDelete = { viewModel.hardDeleteJournalEntry(entry) }
                        )
                    }
                }
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
private fun DeletedItemRow(
    title: String,
    deletedAt: Long,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(deletedAt) {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(deletedAt), ZoneId.systemDefault())
        dt.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("Deleted on $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Permanently Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
