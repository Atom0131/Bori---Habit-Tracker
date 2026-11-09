package com.apagon.rhythm.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import kotlinx.coroutines.delay

/** One AI app's setup: where the text goes and the text itself. */
internal data class AiAppSetup(val name: String, val where: String, val text: String)

/**
 * The setup text for each kind of AI app. Every one runs Bori's own launcher with `--mcp`
 * (BoriMcpServer), so the user installs nothing else. Pure so it can be tested.
 */
internal fun aiAppSetups(command: String, isWindows: Boolean): List<AiAppSetup> {
    // Both JSON and YAML double-quoted strings need backslashes (Windows paths) escaped.
    val quoted = "\"" + command.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    val shellQuoted = if (command.any { it == ' ' || it == '\\' }) "\"$command\"" else command
    val json = """
        {
          "mcpServers": {
            "bori": {
              "command": $quoted,
              "args": ["--mcp"]
            }
          }
        }
    """.trimIndent()
    return listOf(
        AiAppSetup(
            name = "Hermes",
            where = "Add this to ${if (isWindows) "%USERPROFILE%\\.hermes\\config.yaml" else "~/.hermes/config.yaml"} " +
                "(if there is already an mcp_servers: section, put the bori part under it), then restart Hermes. " +
                "Hermes then has Bori in every chat, including Telegram and Discord.",
            text = "mcp_servers:\n  bori:\n    command: $quoted\n    args: [\"--mcp\"]"
        ),
        AiAppSetup(
            name = "Claude Code",
            where = "Run this once in a terminal. Claude Code then has Bori in every project.",
            text = "claude mcp add --scope user bori -- $shellQuoted --mcp"
        ),
        AiAppSetup(
            name = "Claude Desktop & others",
            where = "Claude Desktop: Settings → Developer → Edit Config. Cursor: ~/.cursor/mcp.json. Other apps that " +
                "support MCP use the same \"mcpServers\" format. Merge this in, save, and restart the app.",
            text = json
        ),
    )
}

/** Settings → Obsidian & AI → "Connect an AI app": copy-paste setup for Hermes, Claude and other MCP apps. */
@Composable
internal fun ConnectAiAppDialog(command: String, isWindows: Boolean, onDismiss: () -> Unit) {
    val setups = remember(command, isWindows) { aiAppSetups(command, isWindows) }
    var selected by remember { mutableStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val setup = setups[selected]

    LaunchedEffect(copied) { if (copied) { delay(2000); copied = false } }

    RhythmAlertDialog(
        onDismissRequest = onDismiss,
        maxWidth = 640.dp,
        title = { Text("Connect an AI app") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Lets an AI assistant on this computer see your agenda and add events, to-dos, reminders, habit " +
                        "check-ins, notes, timers and alarms. It asks you before deleting anything. Keep Bori open with " +
                        "\"Allow local AI agents\" on.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    setups.forEachIndexed { i, s ->
                        FilterChip(selected = i == selected, onClick = { selected = i; copied = false }, label = { Text(s.name) })
                    }
                }
                Text(setup.where, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(
                            setup.text,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString(setup.text)); copied = true }) {
                Text(if (copied) "Copied" else "Copy")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
