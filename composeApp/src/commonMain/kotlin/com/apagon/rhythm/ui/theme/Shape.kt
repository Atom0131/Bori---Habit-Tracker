package com.apagon.rhythm.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * One shape scale per theme style, ported from the Android original. These feed
 * `MaterialTheme.shapes`, so stock Material components pick up the right radii with no call site
 * knowing which theme is active. `CrystalShapes` is not ported yet — Stage 14 Phase 1 covers only
 * Material3 and Expressive.
 */

/** Rhythm's own scale, unchanged. The jump from 16dp to 32dp at `large` is deliberate: `large` is
 * what the app's cards use. */
val Material3Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(48.dp)
)

/** Material 3 Expressive runs fuller and rounder than baseline Material across the whole scale —
 * see the Android original's Shape.kt for the full history of why this needed correcting once. */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small      = RoundedCornerShape(18.dp),
    medium     = RoundedCornerShape(26.dp),
    large      = RoundedCornerShape(40.dp),
    extraLarge = RoundedCornerShape(56.dp)
)

/** Crystal leans on generous but continuous corners — softer than Rhythm's own scale at the small
 * end, tighter at the large end (a 32dp card corner turns the specular rim into a racetrack). */
val CrystalShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small      = RoundedCornerShape(14.dp),
    medium     = RoundedCornerShape(20.dp),
    large      = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)
