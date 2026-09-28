package com.apagon.rhythm.platform

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Real backdrop blur on desktop, via the multiplatform `dev.chrisbanes.haze:haze` coordinate
 * (resolves to `haze-jvm` for this source set — NOT `haze-android`), which ships its own
 * Skiko-backed `RenderEffect` blur path for non-Android targets. This was previously assumed
 * impossible and stubbed permanently to branch (c) (the alpha-only fallback); that assumption
 * was never actually checked (see the Stage 14 port plan's own "genuine open question, not
 * assumed either way" note) and was wrong — confirmed by downloading the real `haze-jvm-1.5.3.jar`
 * from Maven Central and inspecting it with `javap`: `HazeSourceNode_skikoKt`,
 * `HazeChildNode_skikoKt`, `RenderEffect_skikoKt` and `RenderEffectBlurEffect` are all present,
 * mirroring the same `selectBlurEffect` fallback-selection logic `CLAUDE.md` documents for
 * Android's `SDK_INT` check, just evaluated for this platform instead.
 *
 * Parameter names/shape here (`HazeStyle(backgroundColor, tints, blurRadius, noiseFactor,
 * fallbackTint)`) match the Android original's `crystalHazeStyle` verbatim — same library,
 * same version (1.5.3), same API.
 */
class DesktopGlassBlur : GlassBlur {
    private class RealField(val hazeState: HazeState) : GlassBlurField

    @Composable
    override fun rememberField(): GlassBlurField = remember { RealField(HazeState()) }

    override fun sourceModifier(field: GlassBlurField, modifier: Modifier): Modifier =
        modifier.hazeSource(state = (field as RealField).hazeState)

    override fun effectModifier(
        field: GlassBlurField?,
        tintColor: Color,
        fallbackAlpha: Float,
        blurRadius: Dp,
        modifier: Modifier
    ): Modifier {
        if (field == null) {
            // No blur source in scope at all (e.g. a Solid background) — same flat
            // alpha-compensation fallback both platforms use in this branch.
            return modifier.background(tintColor.copy(alpha = fallbackAlpha))
        }
        return modifier.hazeEffect(
            state = (field as RealField).hazeState,
            style = HazeStyle(
                backgroundColor = tintColor.copy(alpha = 1f),
                tints = listOf(HazeTint(tintColor)),
                blurRadius = blurRadius,
                noiseFactor = 0.05f,
                fallbackTint = HazeTint(tintColor.copy(alpha = fallbackAlpha))
            )
        )
    }
}
