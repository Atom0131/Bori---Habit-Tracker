package com.apagon.rhythm.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders `text` as a QR code image (Stage 13 follow-up — pairing by camera
 * scan instead of typing an address by hand). Desktop actual uses zxing-core
 * (JVM-only), so this needs the same DI-interface treatment as
 * ImageBitmapLoader/FilePicker rather than a direct call — commonMain can't
 * see desktopMain declarations. No Android actual: the phone only scans, it
 * never needs to render one itself.
 */
interface QrCodeRenderer {
    @Composable
    fun QrCodeImage(text: String, modifier: Modifier)
}
