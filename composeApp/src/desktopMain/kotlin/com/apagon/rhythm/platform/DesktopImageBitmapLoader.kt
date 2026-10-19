package com.apagon.rhythm.platform

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import javax.imageio.ImageIO

// Desktop actual: decodes a plain absolute file path via javax.imageio (no
// content:// URI scheme on the JVM, and coil3 is androidMain-only per
// build.gradle.kts) into a java.awt.image.BufferedImage, then bridges into
// Compose's ImageBitmap via the standard AWT interop. Failures (missing
// file, unreadable format) render nothing rather than crashing the row.
class DesktopImageBitmapLoader : ImageBitmapLoader {
    @Composable
    override fun LoadedImage(path: String, modifier: Modifier, contentScale: ContentScale) {
        val bitmap: ImageBitmap? = remember(path) {
            // By name when the path is another device's (a picture that arrived by sync or restore).
            runCatching { ImageIO.read(PictureFiles.resolve(path))?.toComposeImageBitmap() }.getOrNull()
        }
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = modifier, contentScale = contentScale)
        }
    }
}
