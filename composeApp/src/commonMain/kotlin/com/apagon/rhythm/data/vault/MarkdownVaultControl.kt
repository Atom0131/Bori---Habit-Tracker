package com.apagon.rhythm.data.vault

import kotlinx.coroutines.flow.StateFlow

/**
 * Settings' handle on the desktop Markdown vault: a folder of plain `.md` files (an Obsidian vault)
 * that Bori keeps in two-way sync with a notebook. Implemented by `DesktopNoteVault`.
 */
interface MarkdownVaultControl {
    val state: StateFlow<MarkdownVaultState>

    /** Opens the system folder picker; a chosen folder turns the vault on. */
    fun chooseFolder()
    fun setEnabled(enabled: Boolean)
    fun syncNow()
}

data class MarkdownVaultState(
    val enabled: Boolean = false,
    val folder: String? = null,
    val noteCount: Int = 0,
    val lastSyncAt: Long? = null,
    val lastError: String? = null
)
