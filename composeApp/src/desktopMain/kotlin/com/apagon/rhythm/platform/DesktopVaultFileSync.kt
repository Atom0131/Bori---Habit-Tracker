package com.apagon.rhythm.platform

import com.apagon.rhythm.data.sync.VaultFileDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Base64

/**
 * Stage 3.5 — desktop has no Markdown-vault mirroring system of its own (unlike Android's
 * `NoteVaultRepository`), so this is a deliberately minimal, passive **receive-only** mirror:
 * incoming vault files from a paired phone are written verbatim under `~/.rhythm/vault_mirror/`
 * (or `-Drhythm.home=<dir>` in tests — same property `DesktopPhotoStorage` reads), preserving
 * their relative path. Nothing here reads these files back into any desktop UI or feature — that
 * would be new desktop functionality outside this stage's stated scope (see the Stage 3.5 plan's
 * own scope note on preferring this over building a desktop vault feature from scratch).
 *
 * [exportFiles] always returns empty: desktop has nothing of its own to send back. That is a
 * documented, intentional asymmetry with Android (which has a real vault to export from), not an
 * oversight — see `SyncEngine.buildOutgoingBatch`'s own comment at its call site.
 */
class DesktopVaultFileSync : VaultFileSync {

    private fun mirrorRoot(): File {
        val homeDir = System.getProperty("rhythm.home")?.let { File(it) }
            ?: File(System.getProperty("user.home"), ".rhythm")
        return File(homeDir, "vault_mirror").also { it.mkdirs() }
    }

    override suspend fun exportFiles(): VaultFileSyncExport = VaultFileSyncExport(emptyList(), 0)

    override suspend fun writeIncomingFiles(files: List<VaultFileDto>): Int = withContext(Dispatchers.IO) {
        if (files.isEmpty()) return@withContext 0
        val root = mirrorRoot()
        var written = 0
        for (dto in files) {
            // Defensive: reject a relative path that could escape the mirror root (".." segments,
            // a leading "/") before it ever reaches java.io.File — this writes to the real
            // filesystem from data a peer supplied over the network.
            val segments = dto.relativePath.split('/', '\\').filter { it.isNotBlank() && it != "." }
            if (segments.isEmpty() || segments.any { it == ".." }) continue
            val dest = segments.fold(root) { dir, segment -> File(dir, segment) }
            if (dest.exists() && dest.lastModified() >= dto.mtime) continue // local is newer-or-tied
            val bytes = runCatching { Base64.getDecoder().decode(dto.contentBase64) }.getOrNull() ?: continue
            runCatching {
                dest.parentFile?.mkdirs()
                dest.writeBytes(bytes)
                dest.setLastModified(dto.mtime)
            }.onSuccess { written++ }
        }
        written
    }
}
