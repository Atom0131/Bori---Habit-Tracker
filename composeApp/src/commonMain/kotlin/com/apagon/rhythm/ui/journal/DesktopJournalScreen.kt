package com.apagon.rhythm.ui.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.repository.LockType
import com.apagon.rhythm.ui.components.crystalBareTextFieldColors
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalFabContainerColor
import com.apagon.rhythm.ui.components.crystalFabContentColor
import com.apagon.rhythm.ui.components.crystalFabElevation
import com.apagon.rhythm.ui.components.crystalFabSurface
import org.koin.compose.viewmodel.koinViewModel

/**
 * Desktop port of JournalScreen.kt (Stage 9). No Settings entry point (no
 * desktop Settings screen exists yet — Stage 11) and no Pro paywall (desktop
 * is unconditionally Pro), so onLimitExceeded/paywall branches from the
 * shared JournalViewModel simply never fire here — there's nothing left to
 * wire them to.
 */
@Composable
fun DesktopJournalScreen(viewModel: JournalViewModel = koinViewModel()) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val entries by viewModel.displayEntries.collectAsState()
    val activeHabits by viewModel.activeHabits.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val lockType by viewModel.lockType.collectAsState()
    val journalPin by viewModel.journalPin.collectAsState()
    val journalPassword by viewModel.journalPassword.collectAsState()

    var showNewEntry by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var showLockSettings by remember { mutableStateOf(false) }
    var showUnlockPrompt by remember { mutableStateOf(false) }
    var calendarExpanded by remember { mutableStateOf(false) }

    if (showUnlockPrompt) {
        DesktopJournalLockScreen(
            lockType = lockType,
            onUnlock = { if (viewModel.unlock(it)) showUnlockPrompt = false },
            onCancel = { showUnlockPrompt = false }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .crystalCardSurface().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search journal...") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        TextButton(onClick = { viewModel.setSearchQuery("") }) { Text("×") }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = crystalBareTextFieldColors(),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { showLockSettings = true }) {
                Text(if (lockType == LockType.NONE) "🔓" else "🔒")
            }
        }

        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize()) {
                if (searchQuery.isBlank()) {
                    item(key = "week_strip") {
                        DesktopJournalWeekStrip(
                            selectedDate = selectedDate,
                            onDateSelected = { viewModel.setSelectedDate(it) },
                            expanded = calendarExpanded,
                            onToggleExpanded = { calendarExpanded = !calendarExpanded }
                        )
                    }
                }

                if (entries.isEmpty()) {
                    item(key = "empty_state") {
                        DesktopJournalEmptyState(searchQuery)
                    }
                } else {
                    items(entries, key = { "entry_${it.id}" }) { entry ->
                        DesktopJournalEntryCard(
                            entry = entry,
                            habits = activeHabits,
                            isLocked = isLocked,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onClick = {
                                if (isLocked) showUnlockPrompt = true
                                else editingEntry = entry
                            }
                        )
                    }
                }
                item(key = "bottom_spacer") { Spacer(Modifier.height(80.dp)) }
            }

            FloatingActionButton(
                onClick = { showNewEntry = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).crystalFabSurface(),
                containerColor = crystalFabContainerColor(),
                contentColor = crystalFabContentColor(),
                elevation = crystalFabElevation()
            ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
        }
    }

    if (showNewEntry) {
        DesktopEntrySheet(
            entry = null,
            selectedDate = selectedDate,
            habits = activeHabits,
            onDelete = { viewModel.deleteEntry(it) },
            onDismiss = { showNewEntry = false },
            onSave = { title, content, feelings, tags, photoUris, habitId ->
                viewModel.saveEntry(
                    date = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    title = title, content = content, mood = 0,
                    feelings = feelings, tags = tags, photoUris = photoUris,
                    habitId = habitId,
                    onSuccess = { showNewEntry = false }
                )
            }
        )
    }

    editingEntry?.let { entry ->
        DesktopEntrySheet(
            entry = entry,
            selectedDate = selectedDate,
            habits = activeHabits,
            onDelete = { viewModel.deleteEntry(it) },
            onDismiss = { editingEntry = null },
            onSave = { title, content, feelings, tags, photoUris, habitId ->
                viewModel.saveEntry(
                    date = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    title = title, content = content, mood = 0,
                    feelings = feelings, tags = tags, photoUris = photoUris,
                    habitId = habitId, existingEntry = entry,
                    onSuccess = { editingEntry = null }
                )
            }
        )
    }

    if (showLockSettings) {
        DesktopLockSettingsSheet(
            currentLockType = lockType,
            currentPin = journalPin,
            currentPassword = journalPassword,
            onDismiss = { showLockSettings = false },
            onSavePin = { viewModel.setPin(it) },
            onSavePassword = { viewModel.setPassword(it) },
            onClearAll = { viewModel.clearAllLocks() }
        )
    }
}

@Composable
private fun DesktopJournalEmptyState(searchQuery: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (searchQuery.isBlank()) "📖" else "🔍", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            if (searchQuery.isBlank()) "No entries yet" else "No results for \"$searchQuery\"",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (searchQuery.isBlank()) "Tap + to start writing your day" else "Try a different keyword",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
