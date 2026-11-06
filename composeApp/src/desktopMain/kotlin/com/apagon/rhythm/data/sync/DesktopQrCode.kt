package com.apagon.rhythm.data.sync

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage

/**
 * Renders `text` (this device's "host:port" sync address) as a QR code
 * bitmap — the phone scans it instead of the user typing the address by
 * hand (Stage 13 follow-up). Pure zxing-core, no AWT/Swing beyond the
 * BufferedImage this app already bridges into Compose elsewhere
 * (DesktopImageBitmapLoader's toComposeImageBitmap() pattern).
 */
fun renderQrCode(text: String, size: Int = 240): BufferedImage {
    // A real quiet zone (4 modules, the spec's minimum) and M correction: fixed-focus tablet cameras
    // couldn't lock onto the old near-borderless code (1.1.8).
    val hints = mapOf(EncodeHintType.MARGIN to 4, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until size) {
        for (y in 0 until size) {
            image.setRGB(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return image
}
