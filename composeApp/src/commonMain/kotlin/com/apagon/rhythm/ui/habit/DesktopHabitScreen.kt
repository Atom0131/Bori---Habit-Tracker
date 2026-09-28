package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.platform.QrCodeRenderer
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalCheckboxColors
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalBareTextFieldColors
import com.apagon.rhythm.ui.components.crystalTileSurface
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// Stage 3's minimal desktop Habit screen — a plain list/add/complete UI
// against Compose Multiplatform's common artifacts (not androidx.compose),
// so this same file is a candidate to share with Android later rather than
// a throwaway. Deliberately not a port of the Android HabitListScreen.kt
// (which genuinely depends on real androidx.compose.* + BackHandler and
// isn't directly shareable) — only its list-grouping/tap-to-complete shape
// is borrowed.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopHabitScreen(viewModel: DesktopHabitViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val peerAddress by viewModel.peerAddress.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    var newHabitName by remember { mutableStateOf("") }
    var showQrCode by remember { mutableStateOf(false) }
    val qrCodeRenderer = koinInject<QrCodeRenderer>()
    // Stage 9: tapping a habit navigates to DesktopHabitDetailScreen (Stats).
    // Plain local state, matching this project's existing showX/editingX
    // toggle pattern rather than a real navigation library (Stage 11 territory).
    var selectedHabitId by remember { mutableStateOf<Long?>(null) }

    selectedHabitId?.let { habitId ->
        DesktopHabitDetailScreen(habitId = habitId, onBack = { selectedHabitId = null })
        return
    }

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = { TopAppBar(title = { Text("Rhythm — ${state.date}") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().crystalCardSurface().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newHabitName,
                    onValueChange = { newHabitName = it },
                    label = { Text("New habit") },
                    colors = crystalBareTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    colors = crystalButtonColors(),
                    onClick = {
                        viewModel.addHabit(newHabitName)
                        newHabitName = ""
                    }
                ) {
                    Text("Add")
                }
            }

            // Stage 13: this device's own address, read-only — the user reads
            // it off this line and types it into the phone's peer-address
            // field (desktop is always the sync server). The QR toggle below
            // is the easier path — same address, scanned instead of typed.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    .crystalCardSurface().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your address: ${viewModel.ownSyncAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showQrCode = !showQrCode }) {
                    Text(if (showQrCode) "Hide QR Code" else "Show QR Code")
                }
            }
            if (showQrCode) {
                qrCodeRenderer.QrCodeImage(
                    text = viewModel.ownSyncAddressForPairing,
                    modifier = Modifier.size(200.dp).padding(top = 8.dp)
                )
            }

            // Stage 4b: local sync test UI, still used for the reverse
            // direction (desktop-initiates-sync) and local dev testing.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    .crystalCardSurface().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = peerAddress,
                    onValueChange = { viewModel.updatePeerAddress(it) },
                    label = { Text("Peer address (host:port)") },
                    colors = crystalBareTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                Button(colors = crystalButtonColors(), onClick = { viewModel.syncNow() }) {
                    Text("Sync")
                }
            }
            if (syncStatus != null) {
                Text(syncStatus!!, modifier = Modifier.padding(top = 4.dp))
            }

            if (state.habits.isEmpty()) {
                Text(
                    "No habits scheduled for today yet — add one above.",
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                    items(state.habits, key = { it.id }) { habit ->
                        val isDone = habit.id in state.completedHabitIds
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .crystalTileSurface().padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = { viewModel.toggleCompletion(habit.id, isDone) },
                                colors = crystalCheckboxColors()
                            )
                            Text(
                                habit.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f).clickable { selectedHabitId = habit.id }
                            )
                        }
                    }
                }
            }
        }
    }
}
