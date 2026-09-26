package com.apagon.rhythm.ui.sync

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.ui.util.EditorialTitle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.datetime.LocalDateTime
import org.koin.compose.viewmodel.koinViewModel

// Stage 13: reached from Settings > Data Management > "Sync with Desktop".
// Android is always the sync client here — the peer address is the
// desktop's own Tailscale IP, which the desktop app's Habit screen shows
// read-only for the user to copy across.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: SyncViewModel = koinViewModel()
) {
    val peerAddress by viewModel.peerAddress.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState()

    // Stage 13 follow-up: scan the QR code the desktop app shows instead of
    // typing its Tailscale address by hand. zxing-android-embedded's
    // CaptureActivity requests CAMERA permission itself if not yet granted.
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.updatePeerAddress(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync with Desktop") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditorialTitle("Connect over Tailscale")

            Text(
                "Scan the QR code shown on the desktop app's Habits screen, or type its address " +
                    "in manually, then tap Sync. Both devices need to be on the same Tailscale network.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = { scanLauncher.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setBeepEnabled(false)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan QR Code")
            }

            OutlinedTextField(
                value = peerAddress,
                onValueChange = { viewModel.updatePeerAddress(it) },
                label = { Text("Desktop address (host:port)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = { viewModel.syncNow() }, modifier = Modifier.fillMaxWidth()) {
                Text("Sync")
            }

            syncStatus?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            if (lastSyncedAt > 0) {
                Text(
                    "Last synced: ${LocalDateTime.ofInstant(kotlin.time.Instant.ofEpochMilli(lastSyncedAt), ZoneId.systemDefault())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
