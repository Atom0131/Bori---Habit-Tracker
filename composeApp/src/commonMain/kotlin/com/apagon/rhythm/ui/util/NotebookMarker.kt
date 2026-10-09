package com.apagon.rhythm.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.materialkolor.hct.Hct

/**
 * Android's notebook glyph (`ui/util/Extensions.kt` `NotebookIcon`), copied path for path so a
 * notebook looks the same on both apps: a solid spine, a solid page block, three ruled lines knocked
 * out of it (EvenOdd is what makes them holes). Desktop drew a 📓 emoji, which renders in its own
 * fixed colours and ignored the notebook's colour entirely.
 */
val NotebookIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Notebook",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(3f, 5f); quadTo(3f, 3f, 5f, 3f); lineTo(7f, 3f); lineTo(7f, 21f); lineTo(5f, 21f); quadTo(3f, 21f, 3f, 19f); close()
            moveTo(8f, 3f); lineTo(19f, 3f); quadTo(21f, 3f, 21f, 5f); lineTo(21f, 19f); quadTo(21f, 21f, 19f, 21f); lineTo(8f, 21f); close()
            moveTo(10.5f, 7.3f); lineTo(18.5f, 7.3f); lineTo(18.5f, 8.7f); lineTo(10.5f, 8.7f); close()
            moveTo(10.5f, 11.3f); lineTo(18.5f, 11.3f); lineTo(18.5f, 12.7f); lineTo(10.5f, 12.7f); close()
            moveTo(10.5f, 15.3f); lineTo(15.5f, 15.3f); lineTo(15.5f, 16.7f); lineTo(10.5f, 16.7f); close()
        }
    }.build()
}

/**
 * Android's `legibleMarkerOn`: the notebook's own hue, with chroma and tone floored so the glyph
 * stays readable on the card (pastels and deep colours otherwise vanish on dark glass).
 */
fun Color.legibleMarkerOn(isDark: Boolean): Color {
    val hct = Hct.fromInt(toArgb())
    val chroma = hct.chroma.coerceAtLeast(30.0)
    val tone = if (isDark) hct.tone.coerceAtLeast(70.0) else hct.tone.coerceAtMost(48.0)
    return Color(Hct.from(hct.hue, chroma, tone).toInt())
}
