package com.apagon.rhythm.platform

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The one place the app's data directory is decided. Database, settings, photos, sync pairing,
 * the vault mirror and the single-instance lock all live under it.
 *
 * **Renamed from `~/.rhythm` to `~/.bori` on 2026-10-09**, with the app. An existing `~/.rhythm` is
 * moved to `~/.bori` once, and a `~/.rhythm` symlink is left pointing at it. The symlink is
 * load-bearing: photos are stored in the database by *absolute* path (`DesktopPhotoStorage`), so
 * every photo added before the rename still says `~/.rhythm/photos/...`. If the move or the link
 * fails, this keeps using `~/.rhythm` rather than half-migrating.
 *
 * `-Dbori.home=/path` overrides the location (tests, a second profile). The old `-Drhythm.home`
 * is still honoured. Before this existed the path was spelled out in seven places, and the sync
 * preferences ignored the override entirely.
 */
object AppHome {
    val dir: File by lazy { resolve().apply { mkdirs() } }

    private fun resolve(): File {
        (System.getProperty("bori.home") ?: System.getProperty("rhythm.home"))?.let { return File(it) }
        val home = System.getProperty("user.home")
        val bori = File(home, ".bori")
        val legacy = File(home, ".rhythm")
        if (bori.exists() || !legacy.exists() || Files.isSymbolicLink(legacy.toPath())) {
            return bori
        }
        return runCatching {
            Files.move(legacy.toPath(), bori.toPath(), StandardCopyOption.ATOMIC_MOVE)
            runCatching { Files.createSymbolicLink(legacy.toPath(), bori.toPath()) }
                .onFailure {
                    // No link means old absolute photo paths would break: put the data back.
                    Files.move(bori.toPath(), legacy.toPath(), StandardCopyOption.ATOMIC_MOVE)
                    throw it
                }
            bori
        }.getOrElse { legacy }
    }
}
