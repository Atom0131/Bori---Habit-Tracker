package com.apagon.rhythm.ui.sync

import androidx.compose.material3.Text
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
    val held = pending ?: return
    if (!seenOnboarding || dismissed) return

    RhythmAlertDialog(
        onDismissRequest = { dismissed = true },
        title = {
            Text(held.peerName?.let { "Use your Bori look from $it?" } ?: "Use your Bori look from your other devices?")
        },
        text = {
            Text(
                "This device is new to sync. Keep the look your other devices already share, or " +
                    "send this device's look to all of them.\n\nTheme, colours and layout. Your habits " +
                    "and notes sync either way."
            )
        },
        confirmButton = {
            TextButton(onClick = { scope.launch { join.useExisting() } }) { Text("Keep existing look") }
        },
        dismissButton = {
            TextButton(onClick = { scope.launch { join.keepThisDevice() } }) { Text("Use this device's look") }
        }
    )
}
