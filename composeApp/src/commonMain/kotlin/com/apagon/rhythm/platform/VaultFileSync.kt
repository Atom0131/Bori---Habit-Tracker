package com.apagon.rhythm.platform

import com.apagon.rhythm.data.sync.VaultFileDto

/**
 * Stage 3.5 — platform hook for optional Markdown-vault *file* sync (separate from Stage 2's
 * always-on Note/Notebook *row* sync). Android has a real vault-mirroring system
 * (`NoteVaultRepository`); desktop does not yet, so [DesktopVaultFileSync] is a deliberately
 * minimal, passive receive-only mirror rather than a UI-facing vault feature of its own — see that
 * class's own KDoc for the honest scope of what desktop does with synced vault files today.
 *
 * This interface itself does not check `ThemePreferences.vaultFileSyncEnabled` — `SyncEngine`
 * checks it at both call sites ([SyncEngine.buildOutgoingBatch] before calling [exportFiles],
 * [SyncEngine.applyIncomingBatch] before calling [writeIncomingFiles]), since both ends of a sync
 * must opt in independently.
 */
interface VaultFileSync {
    /** This platform's own vault files for an outgoing batch. Empty when there's nothing to export
     *  (no vault configured, or — on desktop today — no vault feature at all). Never throws. */
    suspend fun exportFiles(): VaultFileSyncExport

    /** Writes each incoming file into this platform's vault/mirror, skipping (never overwriting) a
     *  local file whose own mtime is >= the incoming one's — last-write-wins by mtime, matching the
     *  row-sync philosophy. Returns how many were actually written. Never throws. */
    suspend fun writeIncomingFiles(files: List<VaultFileDto>): Int
}

data class VaultFileSyncExport(val files: List<VaultFileDto>, val skippedTooLarge: Int)

/** Base64-over-one-JSON-blob is not an efficient transport for large binaries — a file above this
 *  cap is skipped (and reported via [VaultFileSyncExport.skippedTooLarge]) rather than attempted.
 *  A real file-transfer protocol is future work, not this pass. */
const val MAX_VAULT_FILE_SYNC_BYTES = 10L * 1024 * 1024
