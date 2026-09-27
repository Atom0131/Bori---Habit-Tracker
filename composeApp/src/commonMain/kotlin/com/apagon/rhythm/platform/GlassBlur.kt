package com.apagon.rhythm.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Abstracts Crystal's frosted-glass blur (Stage 14) behind the same
 * DI-interface pattern as QrCodeRenderer/ImageBitmapLoader — commonMain can't
 * see the Android-only `dev.chrisbanes.haze` library directly.
 *
 * Real backdrop blur (`RenderEffect`, API 32+) only exists on Android — see
 * the Android original's CLAUDE.md "Haze cannot blur across windows" section
 * and its three-way branch (real blur / Solid pre-composite / old-API alpha
 * fallback). Branch (a), real blur, is unreachable on desktop by
 * construction: `DesktopGlassBlur` only ever implements branch (c), the
 * translucent alpha-compensation fallback Crystal already falls back to
 * below API 32 today. Branch (b), Solid pre-compositing, does not go through
 * this interface at all on either platform — it is a separate code path in
 * the surface-tier kit (Phase 4).
 */
interface GlassBlur {
    /** Creates the per-window blur field. Null means "no blur source" (e.g. a Solid background). */
    @Composable
    fun rememberField(): GlassBlurField

    /** Marks [modifier]'s content as a source other surfaces in the same field can blur. */
    fun sourceModifier(field: GlassBlurField, modifier: Modifier): Modifier

    /**
     * Applies the glass effect to [modifier]: real blur tinted by [tintColor] (its own alpha is
     * the tint strength; the blurred background itself is always forced opaque — invariant #4,
     * "crystalHazeStyle forces its base opaque and takes opacity from tintAlpha alone") where
     * available, else [fallbackAlpha]-boosted translucency over [tintColor] alone.
     */
    fun effectModifier(
        field: GlassBlurField?,
        tintColor: Color,
        fallbackAlpha: Float,
        blurRadius: Dp = 12.dp,
        modifier: Modifier
    ): Modifier
}

/** Opaque per-platform blur-field handle — a real `HazeState` on Android, a no-op marker on desktop. */
interface GlassBlurField
