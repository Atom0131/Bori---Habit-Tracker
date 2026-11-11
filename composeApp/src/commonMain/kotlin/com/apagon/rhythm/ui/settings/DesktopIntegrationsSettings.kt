package com.apagon.rhythm.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.time.System
import com.apagon.rhythm.data.agent.AgentApiControl
import com.apagon.rhythm.data.vault.MarkdownVaultControl
import com.apagon.rhythm.ui.components.crystalSwitchColors
import org.koin.compose.koinInject

/**
 * Settings → Obsidian & AI. The Markdown vault: one folder of `.md` files (an Obsidian vault) kept in
 * two-way sync with a notebook, so Obsidian, Bori and a local AI agent all work on the same notes.
 */
@Composable
internal fun IntegrationsSectionContent() {
    val vault = koinInject<MarkdownVaultControl>()
    val state by vault.state.collectAsState()
    val agent = koinInject<AgentApiControl>()
    val agentState by agent.state.collectAsState()
    var showConnect by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SettingsGroup(
            header = "Markdown vault",
            footer = "Each note is a plain .md file named after its title; subfolders are notebooks inside it. " +
                "Lines you don't edit in Bori are left exactly as Obsidian wrote them. Notes deleted in Bori go to " +
                "the vault's .trash folder. If a note changes in both places at once, the newer one wins and the other " +
                "is kept in ~/.bori/vault_conflicts."
        ) {
            SettingsItem(
                title = "Sync a Markdown folder",
                description = state.folder?.let { "Keeps the notes in $it and Bori the same, both ways" }
                    ?: "Choose an Obsidian vault or any folder of .md files",
                onClick = { if (state.folder == null) vault.chooseFolder() else vault.setEnabled(!state.enabled) },
                trailing = {
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = { on -> if (state.folder == null) vault.chooseFolder() else vault.setEnabled(on) },
                        colors = crystalSwitchColors()
                    )
                }
            )
            SettingsDivider()
            SettingsItem(
                title = "Folder",
                description = state.folder ?: "None chosen",
                onClick = { vault.chooseFolder() },
                trailing = { SettingsChevron("Change") }
            )
            if (state.enabled) {
                SettingsDivider()
                SettingsItem(
                    title = "Sync now",
                    description = when {
                        state.lastError != null -> "Couldn't sync: ${state.lastError}"
                        else -> "${state.noteCount} notes in sync" +
                            (state.lastSyncAt?.let { " · last checked ${timeAgo(it)}" } ?: "")
                    },
                    onClick = { vault.syncNow() },
                    trailing = { SettingsChevron() }
                )
            }
        }

        SettingsGroup(
            header = "Local AI agent",
            footer = "Lets an AI app on this computer (like Hermes, Claude Desktop or Claude Code) read your agenda and add " +
                "events, to-dos, reminders, habits, alarms and notes. Only programs on this computer can reach it, and " +
                "only with the secret token in ${agentState.tokenFile}. Agents can't delete anything without you confirming; " +
                "deleted items go to Recently Deleted."
        ) {
            SettingsItem(
                title = "Allow local AI agents",
                description = when {
                    agentState.lastError != null -> agentState.lastError
                    agentState.running -> "On · 127.0.0.1:${agentState.port}" +
                        (agentState.lastRequestAt?.let { " · last used ${timeAgo(it)}" } ?: "")
                    else -> "Off"
                },
                onClick = { agent.setEnabled(!agentState.enabled) },
                trailing = {
                    Switch(checked = agentState.enabled, onCheckedChange = { agent.setEnabled(it) }, colors = crystalSwitchColors())
                }
            )
            if (agentState.enabled) {
                SettingsDivider()
                SettingsItem(
                    title = "Connect an AI app",
                    description = "Setup to copy into Hermes, Claude Desktop, Claude Code and other MCP apps",
                    onClick = { showConnect = true },
                    trailing = { SettingsChevron() }
                )
                SettingsDivider()
                SettingsItem(
                    title = "Ask about a wake-up puzzle",
                    description = "When an AI sets an alarm without one, it asks if you'd like a puzzle to turn it off",
                    onClick = { agent.setAskPuzzleOnAlarms(!agentState.askPuzzleOnAlarms) },
                    trailing = {
                        Switch(checked = agentState.askPuzzleOnAlarms, onCheckedChange = { agent.setAskPuzzleOnAlarms(it) }, colors = crystalSwitchColors())
                    }
                )
                SettingsDivider()
                SettingsItem(
                    title = "New token",
                    description = "Agents using the old token stop working until they're given the new one",
                    onClick = { agent.regenerateToken() },
                    trailing = { SettingsChevron() }
                )
            }
        }
    }

    if (showConnect) {
        ConnectAiAppDialog(agentState.mcpCommand, agentState.isWindows, onDismiss = { showConnect = false })
    }
}

private fun timeAgo(at: Long): String {
    val s = (System.currentTimeMillis() - at) / 1000
    return when {
        s < 60 -> "just now"
        s < 3600 -> "${s / 60} min ago"
        else -> "${s / 3600} h ago"
    }
}
