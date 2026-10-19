package com.apagon.rhythm.platform

import com.apagon.rhythm.data.sync.PictureDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Where note and journal pictures live on this computer, and how a stored picture path is found.
 *
 * **A picture is known by its file name, not its path** (2026-10-19). Every picture is saved as
 * `<uuid>.<ext>`, so the name is unique everywhere, but the path a row stores is the one on the
 * device that added it: `notes/images/x.jpg` (relative, the phone), `/data/user/0/…/x.jpg`,
 * `C:\Users\…\x.jpg`, `~/.rhythm/…`. [resolve] tries the path, then the same name in this
 * computer's own folders, which is where sync and backup restore put pictures from elsewhere.
 */
object PictureFiles {
    private val photosDir get() = File(AppHome.dir, "photos")

    /** The folder for [kind]: the same subfolders the editors import into (DesktopPhotoStorage). */
    fun dir(kind: String): File = File(photosDir, if (kind == PictureDto.JOURNAL) "journal/images" else "notes/images")

    private val NAME = Regex("""[A-Za-z0-9][A-Za-z0-9._-]{0,127}\.(jpe?g|png|webp|gif|bmp)""", RegexOption.IGNORE_CASE)

    /** The file name in [stored], or null when it isn't one a picture could have (no paths, no `..`). */
    fun nameOf(stored: String): String? =
        stored.trim().substringAfterLast('/').substringAfterLast('\\').takeIf { NAME.matches(it) }

    fun resolve(stored: String): File {
        val direct = File(stored)
        if (direct.isFile) return direct
        val name = nameOf(stored) ?: return direct
        for (kind in listOf(PictureDto.NOTE, PictureDto.JOURNAL)) {
            File(dir(kind), name).takeIf { it.isFile }?.let { return it }
        }
        return direct
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** The stored paths of a note's IMAGE blocks. A note's content is its blocks as JSON. */
    fun noteImagePaths(content: String): List<String> = runCatching {
        (json.parseToJsonElement(content) as? JsonArray).orEmpty().mapNotNull { block ->
            val obj = block as? JsonObject ?: return@mapNotNull null
            if (obj["type"]?.jsonPrimitive?.content != "IMAGE") return@mapNotNull null
            obj["content"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        }
    }.getOrDefault(emptyList())

    /** A journal entry's photo paths, stored as a JSON array of strings. */
    fun journalPhotoPaths(photoUris: String): List<String> = runCatching {
        (json.parseToJsonElement(photoUris) as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.content.takeIf { s -> s.isNotBlank() } }
    }.getOrDefault(emptyList())

    /**
     * The bytes to send or embed for [file]: as stored when it's already small, otherwise
     * shrunk to [maxDim] on the long edge and re-encoded as JPEG. A picture added on this computer
     * is copied in at full size, and a camera photo is several MB.
     */
    fun bytesForTransfer(file: File, maxDim: Int = 1600, keepUnder: Long = 1_000_000): ByteArray {
        if (file.length() <= keepUnder) return file.readBytes()
        val src = runCatching { ImageIO.read(file) }.getOrNull() ?: return file.readBytes()
        val long = maxOf(src.width, src.height)
        val ratio = if (long > maxDim) maxDim.toDouble() / long else 1.0
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        out.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(src, 0, 0, w, h, java.awt.Color.WHITE, null)
            dispose()
        }
        return ByteArrayOutputStream().also { ImageIO.write(out, "jpg", it) }.toByteArray()
    }

    /** Writes a picture from elsewhere into [kind]'s folder, unless one by that name is already here. */
    fun saveIncoming(name: String, kind: String, bytes: ByteArray): Boolean {
        val safe = nameOf(name) ?: return false
        val dest = File(dir(kind), safe)
        if (dest.exists()) return false
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, "$safe.part")
        tmp.writeBytes(bytes)
        return tmp.renameTo(dest)
    }
}
