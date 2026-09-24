package com.apagon.rhythm.ui.notes

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.apagon.rhythm.ui.theme.interFamily

/**
 * Desktop counterpart to androidMain's NoteFonts.kt — deliberately NOT named
 * the same (resolveNoteFont/resolveNoteBodySize/resolveNoteHeaderSize),
 * since a commonMain declaration with an identical signature to an
 * androidMain one in the same package is a redeclaration clash once the
 * Android target's compilation merges both source sets. Android's version
 * uses a Google-Fonts provider (GMS-dependent, Android-only); desktop has no
 * such provider, so this always resolves to the same bundled Inter family
 * Stage 5's Type.kt already ships via Compose Resources — no custom note
 * font families (Lora/DM Sans/Roboto Mono) on desktop yet, per the roadmap's
 * explicit "ship with a default font first, land NoteFonts properly later"
 * deferral. The two size-lookup functions are pure logic identical to
 * Android's, just re-declared under a non-clashing name for the same reason.
 */
@Composable
fun resolveDesktopNoteFont(@Suppress("UNUSED_PARAMETER") key: String): FontFamily = interFamily()

fun resolveDesktopNoteBodySize(key: String): TextUnit = when (key) {
    "small" -> 13.sp
    "large" -> 19.sp
    else -> 16.sp
}

fun resolveDesktopNoteHeaderSize(key: String): TextUnit = when (key) {
    "small" -> 20.sp
    "large" -> 28.sp
    else -> 24.sp
}
