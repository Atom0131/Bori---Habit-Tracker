package com.apagon.rhythm.platform

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AndroidPhotoStorage(private val context: Context) : PhotoStorage {

    override suspend fun importPhoto(sourceUri: String, subdirectory: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(sourceUri)
                val dir = File(context.filesDir, subdirectory).also { it.mkdirs() }
                val dest = File(dir, "${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                dest.absolutePath
            }.getOrNull()
        }
}
