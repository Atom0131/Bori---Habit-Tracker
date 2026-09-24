package com.apagon.rhythm.ui.theme

import androidx.compose.ui.graphics.Color

internal val habitColorPalette = listOf(
    Color(0xFF6750A4), // 0 Deep Purple
    Color(0xFF006C4C), // 1 Emerald Green
    Color(0xFF0061A4), // 2 Azure Blue
    Color(0xFFB3261E), // 3 Ruby Red
    Color(0xFFBC6000), // 4 Amber/Orange
)

/** Resolves display color using custom ARGB if available, else palette index. */
internal fun resolveDisplayColor(colorIndex: Int, colorArgb: Int?): Color {
    return if (colorArgb != null) {
        Color(colorArgb)
    } else {
        habitColorPalette.getOrElse(colorIndex) { habitColorPalette.first() }
    }
}
