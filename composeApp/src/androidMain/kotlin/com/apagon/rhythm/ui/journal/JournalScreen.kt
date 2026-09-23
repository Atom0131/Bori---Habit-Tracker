package com.apagon.rhythm.ui.journal
import com.apagon.rhythm.core.time.*

import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.repository.LockType
import com.apagon.rhythm.ui.util.*
import com.apagon.rhythm.ui.util.findActivity
import com.apagon.rhythm.core.time.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel = koinViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val entries by viewModel.displayEntries.collectAsState()
    val activeHabits by viewModel.activeHabits.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val lockType by viewModel.lockType.collectAsState()
    val journalPin by viewModel.journalPin.collectAsState()
    val journalPassword by viewModel.journalPassword.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val isPro by viewModel.isPro.collectAsState()

    var showNewEntry by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var showLockSettings by remember { mutableStateOf(false) }
    var showUnlockPrompt by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var paywallReason by remember { mutableStateOf<String?>(null) }
    var calendarExpanded by remember { mutableStateOf(false) }

    if (showUnlockPrompt) {
        JournalLockScreen(
            lockType = lockType,
            onUnlock = {
                if (viewModel.unlock(it)) showUnlockPrompt = false
            },
            biometricEnabled = biometricEnabled,
            onBiometricSuccess = {
                viewModel.unlockWithBiometrics()
                showUnlockPrompt = false
            },
            onCancel = { showUnlockPrompt = false }
        )
    }

    Column(Modifier.fillMaxSize()) {
        // Search Bar + Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search journal...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showLockSettings = true }) {
                Icon(
                    if (lockType == LockType.NONE) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = "Journal Lock Settings",
                    tint = if (lockType == LockType.NONE) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize()) {
                if (searchQuery.isBlank()) {
                    item(key = "week_strip") {
                        JournalWeekStrip(
                            selectedDate = selectedDate,
                            onDateSelected = { viewModel.setSelectedDate(it) },
                            expanded = calendarExpanded,
                            onToggleExpanded = { calendarExpanded = !calendarExpanded }
                        )
                    }
                }

                if (entries.isEmpty()) {
                    item(key = "empty_state") {
                        if (searchQuery.isBlank()) EmptyJournalState()
                        else EmptySearchState(searchQuery)
                    }
                } else {
                    items(entries, key = { "entry_${it.id}" }) { entry ->
                        JournalEntryCard(
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
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New Entry",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    if (showNewEntry) {
        EntrySheet(
            entry = null,
            selectedDate = selectedDate,
            habits = activeHabits,
            isPro = isPro,
            onDelete = { viewModel.deleteEntry(it) },
            onDismiss = { showNewEntry = false },
            onShowPaywall = {
                paywallReason = it
                showPaywall = true
            },
            onSave = { title, content, feelings, tags, photoUris, habitId ->
                viewModel.saveEntry(
                    date = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    title = title, content = content, mood = 0,
                    feelings = feelings, tags = tags, photoUris = photoUris,
                    habitId = habitId,
                    onSuccess = { showNewEntry = false },
                    onLimitExceeded = {
                        paywallReason = it
                        showPaywall = true
                    }
                )
            }
        )
    }

    editingEntry?.let { entry ->
        EntrySheet(
            entry = entry,
            selectedDate = selectedDate,
            habits = activeHabits,
            isPro = isPro,
            onDelete = { viewModel.deleteEntry(it) },
            onDismiss = { editingEntry = null },
            onShowPaywall = {
                paywallReason = it
                showPaywall = true
            },
            onSave = { title, content, feelings, tags, photoUris, habitId ->
                viewModel.saveEntry(
                    date = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    title = title, content = content, mood = 0,
                    feelings = feelings, tags = tags, photoUris = photoUris,
                    habitId = habitId, existingEntry = entry,
                    onSuccess = { editingEntry = null },
                    onLimitExceeded = {
                        paywallReason = it
                        showPaywall = true
                    }
                )
            }
        )
    }

    if (showLockSettings) {
        LockSettingsSheet(
            currentLockType = lockType,
            currentPin = journalPin,
            currentPassword = journalPassword,
            biometricEnabled = biometricEnabled,
            isPro = isPro,
            onDismiss = { showLockSettings = false },
            onShowPaywall = {
                paywallReason = it
                showPaywall = true
            },
            onSavePin = { viewModel.setPin(it) },
            onSavePassword = { viewModel.setPassword(it) },
            onClearAll = { viewModel.clearAllLocks() },
            onToggleBiometric = { viewModel.setBiometricEnabled(it) }
        )
    }

    val context = LocalContext.current
    if (showPaywall) {
        ProPaywallSheet(
            reason = paywallReason,
            onDismiss = {
                showPaywall = false
                paywallReason = null
            },
            onUpgrade = { productId ->
                val activity = context.findActivity()
                if (activity != null) {
                    viewModel.startBillingFlow(productId)
                }
                showPaywall = false
                paywallReason = null
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
private fun EmptyJournalState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📖", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "No entries yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap + to start writing your day",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔍", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "No results for \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Try a different keyword",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
