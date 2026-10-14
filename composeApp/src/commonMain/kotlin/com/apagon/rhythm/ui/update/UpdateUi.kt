package com.apagon.rhythm.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.app.AppInfo
import com.apagon.rhythm.platform.AppUpdates
import com.apagon.rhythm.platform.UpdateState
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.notes.parseInlineMarkdown
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

/**
 * The sidebar card under the sections: "Bori 1.0.6 is ready — Update now / Later", the download's
 * progress, or why an install failed. Shows nothing otherwise.
 */
@Composable
fun UpdateBanner(modifier: Modifier = Modifier) {
    val updates = koinInject<AppUpdates>()
    val state by updates.state.collectAsState()
    var showNotes by remember { mutableStateOf(false) }

    val visible = when (val s = state) {
        is UpdateState.Available -> !s.dismissed
        is UpdateState.Downloading, is UpdateState.Installing -> true
        is UpdateState.Failed -> s.inBanner
        else -> false
    }
    if (!visible) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .crystalTileSurface(fill = MaterialTheme.colorScheme.primaryContainer)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (val s = state) {
            is UpdateState.Available -> {
                Text(
                    "Bori ${s.version} is ready",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "You have ${AppInfo.VERSION}. Bori will close, update and open again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (s.notes.isNotBlank()) {
                    TextButton(onClick = { showNotes = true }, contentPadding = TightPadding) { Text("What's new") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = updates::install, contentPadding = TightPadding) { Text("Update now") }
                    TextButton(onClick = updates::later, contentPadding = TightPadding) { Text("Later") }
                }
                if (showNotes) ReleaseNotesDialog(s.version, s.notes) { showNotes = false }
            }
            is UpdateState.Downloading -> {
                Text(
                    "Downloading Bori ${s.version}…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
            }
            is UpdateState.Installing -> {
                Text(
                    "Installing Bori ${s.version}…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Bori will close and open again by itself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            is UpdateState.Failed -> {
                Text(
                    "Update didn't finish",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(s.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                TextButton(onClick = updates::later, contentPadding = TightPadding) { Text("OK") }
            }
            else -> Unit
        }
    }
}

/** Settings → Data Management → Updates: the update status, when Bori last checked, and Check now. */
@Composable
fun UpdateSettingsRows() {
    val updates = koinInject<AppUpdates>()
    val state by updates.state.collectAsState()
    val lastChecked by updates.lastCheckedAt.collectAsState()
    var showNotes by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val status = when (val s = state) {
            UpdateState.Checking -> "Checking…"
            UpdateState.UpToDate -> "You're on the latest version."
            UpdateState.NotInstalled -> "This copy wasn't installed from a Bori package, so it doesn't update itself."
            is UpdateState.Available -> "Bori ${s.version} is available."
            is UpdateState.Downloading -> "Downloading Bori ${s.version}… ${(s.progress * 100).toInt()}%"
            is UpdateState.Installing -> "Installing Bori ${s.version}…"
            is UpdateState.Failed -> s.message
            UpdateState.Idle -> "Bori checks GitHub for a new version once a week and asks before installing it."
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (lastChecked > 0) {
            Text(
                "Last checked ${formatChecked(lastChecked)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            val s = state
            if (s is UpdateState.Available) {
                Button(onClick = updates::install) { Text("Update now") }
                if (s.notes.isNotBlank()) OutlinedButton(onClick = { showNotes = true }) { Text("What's new") }
                if (showNotes) ReleaseNotesDialog(s.version, s.notes) { showNotes = false }
            } else if (s != UpdateState.NotInstalled) {
                OutlinedButton(
                    onClick = updates::checkNow,
                    enabled = s !is UpdateState.Checking && s !is UpdateState.Downloading && s !is UpdateState.Installing
                ) { Text("Check now") }
            }
        }
    }
}

@Composable
private fun ReleaseNotesDialog(version: String, notes: String, onDismiss: () -> Unit) {
    RhythmAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's new in $version") },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // The release notes are GitHub markdown: "# Bori 1.0.6" (dropped, the title says it),
                // "## Earlier: 1.0.5" headings, "- " bullets, **bold**.
                notes.lines().map { it.trimEnd() }.filter { it.isNotBlank() && !it.startsWith("# ") }.forEach { line ->
                    when {
                        line.startsWith("#") -> Text(
                            line.trimStart('#').trim(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        line.startsWith("- ") || line.startsWith("* ") -> Row {
                            Text("•  ", style = MaterialTheme.typography.bodyMedium)
                            Text(parseInlineMarkdown(line.drop(2)), style = MaterialTheme.typography.bodyMedium)
                        }
                        else -> Text(parseInlineMarkdown(line), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun formatChecked(millis: Long): String {
    val t = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    val month = t.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$month ${t.dayOfMonth}, ${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}

private val TightPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
