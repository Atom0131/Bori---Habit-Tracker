package com.apagon.rhythm.ui.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.sync.PreferencesJoin
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * A new device's first sync: whose look do the devices share from now on? See PreferencesJoin.
 * Waits until onboarding is done; dismissing leaves the question for the next launch, and nothing is
 * applied meanwhile. Same words as the phone.
 */
@Composable
fun JoinSettingsHost(themePreferences: ThemePreferences) {
    val join = koinInject<PreferencesJoin>()
    LaunchedEffect(join) { join.load() }
    val pending by join.pending.collectAsState()
    val seenOnboarding by themePreferences.hasSeenOnboarding.collectAsState(initial = false)
    var dismissed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Computers start on: the user wants alarms on their PCs, only tablets start off (phone's dialog).
    var ringAlarms by remember { mutableStateOf(true) }
    val held = pending ?: return
    if (!seenOnboarding || dismissed) return

    RhythmAlertDialog(
        onDismissRequest = { dismissed = true },
        title = {
            Text(held.peerName?.let { "Use your Bori look from $it?" } ?: "Use your Bori look from your other devices?")
        },
        text = {
            Column {
                Text(
                    "This device is new to sync. Keep the look your other devices already share, or " +
                        "send this device's look to all of them.\n\nTheme, colours and layout. Your habits " +
                        "and notes sync either way."
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable { ringAlarms = !ringAlarms },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = ringAlarms, onCheckedChange = { ringAlarms = it })
                    Column {
                        Text("Ring alarms & timers on this device", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "They sync either way. You can change this in Sync settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { themePreferences.setRingAlarmsHere(ringAlarms); join.useExisting() }
            }) { Text("Keep existing look") }
        },
        dismissButton = {
            TextButton(onClick = {
                scope.launch { themePreferences.setRingAlarmsHere(ringAlarms); join.keepThisDevice() }
            }) { Text("Use this device's look") }
        }
    )
}
