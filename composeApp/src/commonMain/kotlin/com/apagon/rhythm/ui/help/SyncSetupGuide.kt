package com.apagon.rhythm.ui.help

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.sync.OsMark
import com.apagon.rhythm.ui.util.RhythmSheet

/**
 * "Set up sync": how to connect the phone and computers over Tailscale, one tab per device kind.
 * The same steps and wording are in the Android app's SyncSetupGuideSheet; change both together.
 * Tailscale is the one supported way to connect: the sync server only ever listens on this
 * computer's Tailscale address (see SyncAddressMonitor).
 */
object SyncGuideLinks {
    const val RELEASES = "https://github.com/Atom0131/Bori---Habit-Tracker/releases/latest"
    const val TAILSCALE_PLAY = "https://play.google.com/store/apps/details?id=com.tailscale.ipn"
    const val TAILSCALE_WINDOWS = "https://tailscale.com/download/windows"
    const val TAILSCALE_LINUX = "https://tailscale.com/download/linux"
}

private data class GuideStep(
    val title: String,
    val body: String,
    val commands: List<Pair<String, String>> = emptyList(),
    val link: Pair<String, String>? = null
)

private val scanStep = GuideStep(
    "Scan the code with your phone",
    "On the phone, open Bori → Settings → Sync with Computers → Scan QR Code, and point it at the code " +
        "on the computer. Syncing with a computer needs Bori Pro on the phone; the computer app is free."
)

private val phoneSteps = listOf(
    GuideStep("Install Tailscale", "Tailscale is a free private network that connects your own devices, at home or away. Bori uses it to reach your computer directly — nothing goes through a Bori server.",
        link = "Open Tailscale in the Play Store" to SyncGuideLinks.TAILSCALE_PLAY),
    GuideStep("Sign in and turn it on", "Open Tailscale, sign in, and switch it on. Use the same account on every device. A key icon in the status bar means it's connected."),
    GuideStep("Keep it on (Samsung)", "On Samsung phones go to Settings → Apps → Tailscale → Battery and choose Unrestricted, or the phone may switch Tailscale off in the background."),
    scanStep
)

private val windowsSteps = listOf(
    GuideStep("Get Bori for Windows", "Download bori-<version>.msi from the Releases page and open it. If SmartScreen warns about an unrecognised app, choose More info → Run anyway.",
        link = "Open the Releases page" to SyncGuideLinks.RELEASES),
    GuideStep("Install Tailscale for Windows", "Install it, then sign in from the Tailscale icon in the taskbar tray with the same account as your phone.",
        link = "Open tailscale.com" to SyncGuideLinks.TAILSCALE_WINDOWS),
    GuideStep("Open Bori → Settings → Data → Sync", "When it says Ready for your phone, the QR code is shown. The first time, Windows Firewall asks about Bori: allow it."),
    scanStep
)

private val linuxSteps = listOf(
    GuideStep("Get Bori for Linux", "Download the .deb (Ubuntu, Debian, Mint), .rpm (Fedora, openSUSE) or .pkg.tar.zst (Arch, CachyOS) from the Releases page and install it.",
        link = "Open the Releases page" to SyncGuideLinks.RELEASES),
    GuideStep("Install Tailscale", "Run the command for your system in a terminal:",
        commands = listOf(
            "Arch, CachyOS, Manjaro" to "sudo pacman -S tailscale && sudo systemctl enable --now tailscaled",
            "Other distros" to "curl -fsSL https://tailscale.com/install.sh | sh"
        ),
        link = "More install options" to SyncGuideLinks.TAILSCALE_LINUX),
    GuideStep("Sign in", "Run this, open the link it prints, and sign in with the same account as your phone:",
        commands = listOf("" to "sudo tailscale up")),
    GuideStep("Open Bori → Settings → Data → Sync", "When it says Ready for your phone, the QR code is shown. If Bori was already open, it notices Tailscale by itself within about 15 seconds."),
    scanStep
)

private val troubleshooting = listOf(
    "Offline, or \"didn't answer\": Bori has to be open on the computer (closing the window to the tray is fine), and Tailscale has to be on on both devices.",
    "The computer says Tailscale isn't running: start Tailscale and sign in; Bori picks it up within about 15 seconds, no restart needed.",
    "\"Has sync with this device turned off\": that computer has the phone switched off in its Settings → Data → Sync list. Switch it back on there.",
    "Samsung: if sync works only right after opening Tailscale, set Tailscale's battery use to Unrestricted.",
    "Two computers: on one of them, open Settings → Data → Sync → More options, and add the other computer's address (shown under Ready for your phone on that computer).",
    "Nothing changed after a sync: each device's \"What to sync\" switches decide what it sends and receives; check they're on for what you expect."
)

/** The guide as a sheet. [initialTab]: 0 phone, 1 Windows, 2 Linux (default: this computer's OS). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSetupGuideSheet(onDismiss: () -> Unit, initialTab: Int = thisComputerTab()) {
    RhythmSheet(
        onDismiss = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        maxWidth = 640.dp
    ) {
        SyncSetupGuideContent(initialTab, Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp))
    }
}

fun thisComputerTab(): Int =
    if (System.getProperty("os.name").orEmpty().lowercase().contains("windows")) 1 else 2

@Composable
fun SyncSetupGuideContent(initialTab: Int, modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(initialTab) }
    var showTroubleshooting by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Set up sync", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Bori syncs directly between your phone and computers over Tailscale. Do these steps on each device — " +
                "the order doesn't matter, as long as every device signs in to the same Tailscale account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("android" to "Phone", "windows" to "Windows", "linux" to "Linux").forEachIndexed { i, (os, label) ->
                FilterChip(
                    selected = tab == i,
                    onClick = { tab = i },
                    label = { Text(label) },
                    leadingIcon = { OsMark(os, size = 18.dp) }
                )
            }
        }
        val steps = when (tab) { 0 -> phoneSteps; 1 -> windowsSteps; else -> linuxSteps }
        steps.forEachIndexed { i, step -> GuideStepRow(i + 1, step) }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { showTroubleshooting = !showTroubleshooting }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Troubleshooting", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(if (showTroubleshooting) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
        }
        if (showTroubleshooting) {
            troubleshooting.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun GuideStepRow(number: Int, step: GuideStep) {
    val uri = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(28.dp).crystalCardSurface(),
            contentAlignment = Alignment.Center
        ) { Text("$number", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(step.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(step.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            step.commands.forEach { (label, command) ->
                if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.fillMaxWidth().crystalCardSurface().padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectionContainer(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(command, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { clipboard.setText(AnnotatedString(command)) }) { Text("Copy") }
                }
            }
            step.link?.let { (label, url) ->
                OutlinedButton(onClick = { runCatching { uri.openUri(url) } }) { Text(label) }
            }
        }
    }
}
