package com.apagon.rhythm.ui.sync

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The symbol for a sync device's operating system, beside its name in the device list: the
 * four-pane window for Windows, a small Tux for Linux, the Android robot for a phone, and a plain
 * computer when the device hasn't said (versions before 2026-10-13) or is something else. Drawn in
 * code so nothing ships as an image; the same file is in the Android and desktop apps.
 */
@Composable
fun OsMark(os: String?, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    when (os) {
        "windows" -> Canvas(modifier.size(size)) {
            val gap = this.size.width * 0.08f
            val pane = (this.size.width - gap) / 2f
            val blue = Color(0xFF0078D4)
            for (row in 0..1) for (col in 0..1) {
                drawRect(blue, Offset(col * (pane + gap), row * (pane + gap)), Size(pane, pane))
            }
        }
        "linux" -> Canvas(modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val black = Color(0xFF1B1B1B)
            val white = Color(0xFFF5F5F5)
            val yellow = Color(0xFFF5B700)
            fun oval(c: Color, l: Float, t: Float, r: Float, b: Float) =
                drawOval(c, Offset(l * w, t * h), Size((r - l) * w, (b - t) * h))
            oval(yellow, 0.14f, 0.84f, 0.48f, 0.99f)   // feet
            oval(yellow, 0.52f, 0.84f, 0.86f, 0.99f)
            oval(black, 0.20f, 0.04f, 0.80f, 0.93f)    // body and head
            oval(white, 0.30f, 0.40f, 0.70f, 0.90f)    // belly
            oval(white, 0.35f, 0.17f, 0.48f, 0.33f)    // eyes
            oval(white, 0.52f, 0.17f, 0.65f, 0.33f)
            oval(black, 0.40f, 0.22f, 0.46f, 0.30f)    // pupils
            oval(black, 0.54f, 0.22f, 0.60f, 0.30f)
            oval(yellow, 0.39f, 0.31f, 0.61f, 0.41f)   // beak
        }
        "android" -> Icon(Icons.Filled.Android, contentDescription = "Android", tint = Color(0xFF3DDC84), modifier = modifier.size(size))
        else -> Icon(Icons.Filled.Computer, contentDescription = "Computer", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.size(size))
    }
}
