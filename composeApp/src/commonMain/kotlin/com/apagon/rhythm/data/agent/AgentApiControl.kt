package com.apagon.rhythm.data.agent

import kotlinx.coroutines.flow.StateFlow

/**
 * Settings' handle on the local AI agent API: a loopback-only HTTP endpoint a local agent (Coqui
 * through `bori --mcp`) uses to read the agenda and add events, to-dos and reminders.
 * Implemented by `AgentApiServer` on the desktop.
 */
interface AgentApiControl {
    val state: StateFlow<AgentApiState>
    fun setEnabled(enabled: Boolean)
    /** Replaces the token; agents holding the old one stop working. */
    fun regenerateToken()
}

data class AgentApiState(
    val enabled: Boolean = false,
    val running: Boolean = false,
    val port: Int = 47891,
    val tokenFile: String = "",
    val lastError: String? = null,
    val lastRequestAt: Long? = null,
    /** Full path of this Bori's own launcher; an AI app runs it with `--mcp` (see BoriMcpServer). */
    val mcpCommand: String = "bori",
    val isWindows: Boolean = false
)
