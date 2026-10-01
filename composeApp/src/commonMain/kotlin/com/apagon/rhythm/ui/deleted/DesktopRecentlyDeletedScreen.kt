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
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.ui.components.crystalTileSurface
import org.koin.compose.viewmodel.koinViewModel

// Was Stage 7's deliberately-narrow Habit+Todo-only trash, covering just the two entities that
// existed on desktop at the time. Every entity DesktopRecentlyDeletedViewModel was waiting on
// (Reminders, Calendar, Alarms, Timers, Notes, Journal) has since landed on desktop — Stages 8-12
// added them all, and DesktopBackupManager already round-trips every one of them — so this now
// uses the same full RecentlyDeletedViewModel/DeletedItems the rest of the app's shared code was
// already written against, instead of the narrowed desktop-only pair.
@Composable
fun DesktopRecentlyDeletedScreen(viewModel: RecentlyDeletedViewModel = koinViewModel()) {
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
        if (items.reminders.isNotEmpty()) {
            item { DeletedSectionHeader("Reminders") }
            items(items.reminders, key = { "reminder-${it.id}" }) { reminder: Reminder ->
                DesktopDeletedItemRow(
                    title = reminder.title,
                    onRestore = { viewModel.restoreReminder(reminder) },
                    onDelete = { viewModel.hardDeleteReminder(reminder) }
                )
            }
        }
        if (items.events.isNotEmpty()) {
            item { DeletedSectionHeader("Events") }
            items(items.events, key = { "event-${it.id}" }) { event: CalendarEvent ->
                DesktopDeletedItemRow(
                    title = event.title,
                    onRestore = { viewModel.restoreEvent(event) },
                    onDelete = { viewModel.hardDeleteEvent(event) }
                )
            }
        }
        if (items.alarms.isNotEmpty()) {
            item { DeletedSectionHeader("Alarms") }
            items(items.alarms, key = { "alarm-${it.id}" }) { alarm: Alarm ->
                DesktopDeletedItemRow(
                    title = alarm.label.ifBlank { "Alarm" },
                    onRestore = { viewModel.restoreAlarm(alarm) },
                    onDelete = { viewModel.hardDeleteAlarm(alarm) }
                )
            }
        }
        if (items.timers.isNotEmpty()) {
            item { DeletedSectionHeader("Timers") }
            items(items.timers, key = { "timer-${it.id}" }) { timer: Timer ->
                DesktopDeletedItemRow(
                    title = timer.label.ifBlank { "Timer" },
                    onRestore = { viewModel.restoreTimer(timer) },
                    onDelete = { viewModel.hardDeleteTimer(timer) }
                )
            }
        }
        if (items.notebooks.isNotEmpty()) {
            item { DeletedSectionHeader("Notebooks") }
            items(items.notebooks, key = { "notebook-${it.id}" }) { notebook: Notebook ->
                DesktopDeletedItemRow(
                    title = notebook.name,
                    onRestore = { viewModel.restoreNotebook(notebook) },
                    onDelete = { viewModel.hardDeleteNotebook(notebook) }
                )
            }
        }
        if (items.notes.isNotEmpty()) {
            item { DeletedSectionHeader("Notes") }
            items(items.notes, key = { "note-${it.id}" }) { note: Note ->
                DesktopDeletedItemRow(
                    title = note.title.ifBlank { "Untitled note" },
                    onRestore = { viewModel.restoreNote(note) },
                    onDelete = { viewModel.hardDeleteNote(note) }
                )
            }
        }
        if (items.journalEntries.isNotEmpty()) {
            item { DeletedSectionHeader("Journal") }
            items(items.journalEntries, key = { "journal-${it.id}" }) { entry: JournalEntry ->
                DesktopDeletedItemRow(
                    title = entry.title.ifBlank { entry.date },
                    onRestore = { viewModel.restoreJournalEntry(entry) },
                    onDelete = { viewModel.hardDeleteJournalEntry(entry) }
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
