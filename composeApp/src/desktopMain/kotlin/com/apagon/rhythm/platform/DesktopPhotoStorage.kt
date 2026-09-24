package com.apagon.rhythm.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

// Desktop actual for PhotoStorage. There's no content:// URI scheme on the
// JVM — a desktop file picker (Stage 9/10) resolves straight to an absolute
// path, so sourceUri here is just that path — copied into ~/.rhythm/photos/
// the same way AndroidPhotoStorage copies into filesDir, so it stays
// readable independent of wherever the source file lives.
class DesktopPhotoStorage : PhotoStorage {

    override suspend fun importPhoto(sourceUri: String, subdirectory: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = File(sourceUri)
                if (!source.isFile) return@runCatching null
                val homeDir = System.getProperty("rhythm.home")?.let { File(it) }
                    ?: File(System.getProperty("user.home"), ".rhythm")
                val dir = File(File(homeDir, "photos"), subdirectory).also { it.mkdirs() }
                val dest = File(dir, "${UUID.randomUUID()}${source.extension.let { if (it.isNotEmpty()) ".$it" else ".jpg" }}")
                source.copyTo(dest, overwrite = true)
                dest.absolutePath
            }.getOrNull()
        }
}
