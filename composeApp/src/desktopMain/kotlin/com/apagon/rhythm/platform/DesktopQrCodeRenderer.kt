package com.apagon.rhythm.platform

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.apagon.rhythm.data.sync.renderQrCode

class DesktopQrCodeRenderer : QrCodeRenderer {
    @Composable
    override fun QrCodeImage(text: String, modifier: Modifier) {
        val bitmap = remember(text) { renderQrCode(text, size = 512).toComposeImageBitmap() }
        // Hard pixel edges at any scale: a smoothed QR is exactly what a weak camera fails on.
        Image(bitmap = bitmap, contentDescription = "QR code for $text", filterQuality = FilterQuality.None, modifier = modifier)
    }
}
