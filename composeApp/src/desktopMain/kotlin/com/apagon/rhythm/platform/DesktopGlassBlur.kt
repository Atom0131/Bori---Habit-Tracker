package com.apagon.rhythm.platform

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Desktop has no `android.graphics.RenderEffect` equivalent wired up here, so
 * real backdrop blur (branch (a) of Crystal's three-way fallback) is
 * unreachable by construction. This implements only branch (c) — the
 * translucent alpha-compensation fallback Crystal already uses below API 32
 * on Android — not a new invention. Branch (b) (Solid pre-compositing) is a
 * separate code path outside this interface on both platforms.
 */
class DesktopGlassBlur : GlassBlur {
    private object NoOpField : GlassBlurField

    @Composable
    override fun rememberField(): GlassBlurField = NoOpField

    override fun sourceModifier(field: GlassBlurField, modifier: Modifier): Modifier =
        modifier // nothing to register — there is no real blur consumer on this platform

    override fun effectModifier(
        field: GlassBlurField?,
        tintColor: Color,
        fallbackAlpha: Float,
        blurRadius: Dp,
        modifier: Modifier
    ): Modifier = modifier.background(tintColor.copy(alpha = fallbackAlpha))
}
