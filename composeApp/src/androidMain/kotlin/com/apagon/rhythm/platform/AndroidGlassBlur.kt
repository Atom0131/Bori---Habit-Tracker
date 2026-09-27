package com.apagon.rhythm.platform

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Real backdrop blur via `dev.chrisbanes.haze` 1.5.3.
 *
 * That library compiles against an older Compose ABI than this project
 * resolves (see the version-catalog comment on `haze`). Verified compatible
 * for this Compose Multiplatform BOM by extracting Haze's referenced Compose
 * symbols with `javap` and cross-checking them against the resolved
 * `androidx.compose.ui:ui` artifact (matching the Android original's own
 * methodology, CLAUDE.md's "Haze (dev.chrisbanes.haze)" section) — re-run
 * that check rather than trusting a clean compile if the Compose BOM here
 * ever moves; a compile proves nothing about a library built against an
 * older ABI, and the failure mode is a runtime `NoSuchMethodError`, not a
 * build error.
 */
class AndroidGlassBlur : GlassBlur {
    private class AndroidBlurField(val hazeState: HazeState) : GlassBlurField

    @Composable
    override fun rememberField(): GlassBlurField = AndroidBlurField(remember { HazeState() })

    override fun sourceModifier(field: GlassBlurField, modifier: Modifier): Modifier {
        val hazeState = (field as AndroidBlurField).hazeState
        return modifier.hazeSource(hazeState)
    }

    override fun effectModifier(
        field: GlassBlurField?,
        tintColor: Color,
        fallbackAlpha: Float,
        blurRadius: Dp,
        modifier: Modifier
    ): Modifier {
        val hazeState = (field as? AndroidBlurField)?.hazeState
            ?: return modifier.background(tintColor.copy(alpha = fallbackAlpha))
        // Invariant #4: the blurred backdrop is always forced opaque — `tintColor`'s own alpha is
        // the tint strength, not the background's. Handing a translucent role straight to
        // `backgroundColor` double-multiplies and the glass tint disappears.
        val opaqueBase = tintColor.copy(alpha = 1f)
        return modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = opaqueBase,
                tints = listOf(HazeTint(tintColor)),
                blurRadius = blurRadius,
                noiseFactor = 0.05f,
                fallbackTint = HazeTint(opaqueBase.copy(alpha = fallbackAlpha))
            )
        )
    }
}
