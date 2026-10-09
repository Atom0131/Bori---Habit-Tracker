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
import com.apagon.rhythm.ui.components.CrystalIconButton
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.components.crystalBareTextFieldColors
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.util.RhythmAddFab
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close

/** Stage 15d: which of the two panes' selection state the right column shows, if any. */
private sealed class JournalEditorState {
    object None : JournalEditorState()
    object New : JournalEditorState()
    data class Editing(val entry: JournalEntry) : JournalEditorState()
}

/**
 * Desktop port of JournalScreen.kt (Stage 9). No Settings entry point (no
 * desktop Settings screen exists yet — Stage 11) and no Pro paywall (desktop
 * is unconditionally Pro), so onLimitExceeded/paywall branches from the
 * shared JournalViewModel simply never fire here — there's nothing left to
 * wire them to.
 *
 * Stage 15d: split into a left column (search/lock row + week-strip + entry
 * list, unchanged) and a right column showing the selected/new entry inline
 * via DesktopEntryEditorPane — replacing the previous full-screen Dialog
 * overlay so the entry list stays visible while editing, the same
 * master-detail pattern Stage 15c applied to Notes.
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

    var editorState by remember { mutableStateOf<JournalEditorState>(JournalEditorState.None) }
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

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            // Stage 16: same centered content-width cap every other primary screen got — wraps
            // just the search row + list, not the FAB below, so the FAB still pins to this pane's
            // true bottom-right corner rather than trailing the capped column's edge.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Row(
                modifier = Modifier.widthIn(max = DesktopLayout.contentMaxWidth).fillMaxWidth()
                    .padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing)
                    .crystalCardSurface().padding(DesktopLayout.compactCardPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search journal...") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            CrystalIconButton(icon = Icons.Default.Close, contentDescription = "Close", onClick = { viewModel.setSearchQuery("") }, size = 28.dp)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = crystalBareTextFieldColors(),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                CrystalIconButton(icon = if (lockType == LockType.NONE) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = "Journal lock", onClick = { showLockSettings = true })
            }
            }

            Box(Modifier.weight(1f)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.fillMaxHeight().widthIn(max = DesktopLayout.contentMaxWidth).fillMaxWidth()) {
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
                                modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = 6.dp),
                                onClick = {
                                    if (isLocked) showUnlockPrompt = true
                                    else editorState = JournalEditorState.Editing(entry)
                                }
                            )
                        }
                    }
                    item(key = "bottom_spacer") { Spacer(Modifier.height(80.dp)) }
                }
                }

                RhythmAddFab(
                    onClick = { editorState = JournalEditorState.New },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(DesktopLayout.screenPadding)
                )
            }
        }

        when (val state = editorState) {
            is JournalEditorState.None -> Unit
            is JournalEditorState.New -> {
                VerticalDivider()
                DesktopEntryEditorPane(
                    entry = null,
                    selectedDate = selectedDate,
                    habits = activeHabits,
                    onDelete = { viewModel.deleteEntry(it) },
                    onDismiss = { editorState = JournalEditorState.None },
                    onSave = { title, content, feelings, tags, photoUris, habitId ->
                        viewModel.saveEntry(
                            date = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            title = title, content = content, mood = 0,
                            feelings = feelings, tags = tags, photoUris = photoUris,
                            habitId = habitId,
                            onSuccess = { editorState = JournalEditorState.None }
                        )
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            is JournalEditorState.Editing -> {
                VerticalDivider()
                DesktopEntryEditorPane(
                    entry = state.entry,
                    selectedDate = selectedDate,
                    habits = activeHabits,
                    onDelete = { viewModel.deleteEntry(it) },
                    onDismiss = { editorState = JournalEditorState.None },
                    onSave = { title, content, feelings, tags, photoUris, habitId ->
                        viewModel.saveEntry(
                            date = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            title = title, content = content, mood = 0,
                            feelings = feelings, tags = tags, photoUris = photoUris,
                            habitId = habitId, existingEntry = state.entry,
                            onSuccess = { editorState = JournalEditorState.None }
                        )
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
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
