package com.apagon.rhythm.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Abstracts Crystal's frosted-glass blur (Stage 14) behind the same
 * DI-interface pattern as QrCodeRenderer/ImageBitmapLoader — commonMain can't
 * see the platform-specific `dev.chrisbanes.haze` API surface directly (real
 * blur is a Skia `RenderEffect` on Android, a Skiko one on desktop — same
 * library, different backend per `AndroidGlassBlur`/`DesktopGlassBlur`).
 *
 * See the Android original's CLAUDE.md "Haze cannot blur across windows" section
 * for the three-way branch (real blur / Solid pre-composite / old-API alpha
 * fallback) both platforms share. **Branch (a), real blur, is NOT unreachable on
 * desktop** — this was assumed during the initial port and left unverified; the
 * `dev.chrisbanes.haze:haze` (not `-android`) multiplatform coordinate resolves to
 * a real `haze-jvm` artifact with its own Skiko-backed `RenderEffect` blur path,
 * confirmed via `javap` against the actual downloaded jar. `DesktopGlassBlur` now
 * uses it. Branch (c) (the translucent alpha-compensation fallback) is still what
 * both platforms use when there is no blur field in scope at all (e.g. a Solid
 * background) — that part of the assumption was correct. Branch (b), Solid
 * pre-compositing, does not go through this interface at all on either platform —
 * it is a separate code path in the surface-tier kit (Phase 4).
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

/** Opaque per-platform blur-field handle — wraps a real `HazeState` on both Android and desktop. */
interface GlassBlurField
