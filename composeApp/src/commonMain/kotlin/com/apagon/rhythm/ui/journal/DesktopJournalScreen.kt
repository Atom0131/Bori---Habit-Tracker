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
import com.apagon.rhythm.ui.components.crystalTextFieldColors
import com.apagon.rhythm.ui.components.crystalIconButtonSurface
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon

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
    // As Android: entry previews are concealed by default; the eye reveals them for this visit,
    // asking for the PIN/password first when a lock is set.
    var revealed by remember { mutableStateOf(false) }
    var revealAfterUnlock by remember { mutableStateOf(false) }
    var calendarExpanded by remember { mutableStateOf(false) }

    if (showUnlockPrompt) {
        DesktopJournalLockScreen(
            lockType = lockType,
            onUnlock = {
                if (viewModel.unlock(it)) {
                    showUnlockPrompt = false
                    if (revealAfterUnlock) revealed = true
                    revealAfterUnlock = false
                }
            },
            onCancel = { showUnlockPrompt = false; revealAfterUnlock = false }
        )
    }

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            // Stage 16: same centered content-width cap every other primary screen got — wraps
            // just the search row + list, not the FAB below, so the FAB still pins to this pane's
            // true bottom-right corner rather than trailing the capped column's edge.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            // Android's header: a search field with a search icon, then glass round buttons for
            // revealing previews and the journal lock (Settings is in the sidebar on desktop).
            Row(
                modifier = Modifier.widthIn(max = DesktopLayout.contentMaxWidth).fillMaxWidth()
                    .padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search journal...", maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = crystalTextFieldColors(),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        when {
                            revealed -> revealed = false
                            isLocked -> { revealAfterUnlock = true; showUnlockPrompt = true }
                            else -> revealed = true
                        }
                    },
                    modifier = Modifier.crystalIconButtonSurface()
                ) {
                    Icon(
                        if (revealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (revealed) "Hide entry previews" else "Show entry previews",
                        tint = if (revealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { showLockSettings = true }, modifier = Modifier.crystalIconButtonSurface()) {
                    Icon(
                        if (lockType == LockType.NONE) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Journal Lock Settings",
                        tint = if (lockType == LockType.NONE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
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
                                concealed = !revealed,
                                locked = lockType != LockType.NONE,
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
