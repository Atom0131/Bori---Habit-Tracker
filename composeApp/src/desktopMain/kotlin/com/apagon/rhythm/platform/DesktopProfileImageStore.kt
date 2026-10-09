package com.apagon.rhythm.platform

import com.apagon.rhythm.data.preferences.ThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.UUID
import javax.imageio.ImageIO

class DesktopProfileImageStore(private val themePreferences: ThemePreferences) : ProfileImageStore {
    private val dir get() = File(File(AppHome.dir, "photos"), "profile")

    override suspend fun export(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val stored = themePreferences.profilePictureUri.first()
            if (stored.isNullOrBlank()) return@runCatching ""
            val image = File(stored).takeIf { it.isFile }?.let { ImageIO.read(it) } ?: return@runCatching null
            Base64.getEncoder().encodeToString(toJpeg(downscale(image, 512)))
        }.getOrNull()
    }

    override suspend fun import(encoded: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                val previous = themePreferences.profilePictureUri.first()
                if (encoded.isEmpty()) {
                    themePreferences.setProfilePictureUriQuietly(null)
                } else {
                    val bytes = Base64.getDecoder().decode(encoded)
                    ImageIO.read(bytes.inputStream()) ?: return@runCatching // not an image
                    dir.mkdirs()
                    val file = File(dir, "${UUID.randomUUID()}.jpg").apply { writeBytes(bytes) }
                    themePreferences.setProfilePictureUriQuietly(file.absolutePath)
                }
                // Drop the replaced picture if it was one this store wrote.
                previous?.let { File(it) }?.takeIf { it.parentFile?.canonicalPath == dir.canonicalPath }?.delete()
            }
        }
    }

    private fun downscale(src: BufferedImage, maxDim: Int): BufferedImage {
        val long = maxOf(src.width, src.height)
        val ratio = if (long > maxDim) maxDim.toDouble() / long else 1.0
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return BufferedImage(w, h, BufferedImage.TYPE_INT_RGB).also { out ->
            out.createGraphics().apply {
                setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                drawImage(src, 0, 0, w, h, null)
                dispose()
            }
        }
    }

    private fun toJpeg(image: BufferedImage): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(image, "jpg", it) }.toByteArray()
}
