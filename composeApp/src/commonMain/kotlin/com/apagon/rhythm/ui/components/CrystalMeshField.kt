package com.apagon.rhythm.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale

/**
 * The blob mesh itself, as a draw call. Ported verbatim from the Android original's
 * `ui/components/Crystal.kt` (`drawCrystalMeshField`) — pure `DrawScope`/`Brush.radialGradient`
 * Compose code, no platform dependency.
 *
 * **Aspect-invariance is load-bearing, not incidental** (Stage 14 plan doc invariant #9): each
 * blob's radius is a fraction of its *own* axis (`fieldSize.width * rad`, `fieldSize.height * rad`),
 * not a single scalar off `maxOf(width, height)` — the earlier, buggy version of this function
 * produced a genuinely different picture on a square swatch vs. a tall phone screen from the exact
 * same code. This port preserves the fixed, per-axis version. Confirmed empirically for this port:
 * see the Stage 14 fork's Phase 2 report for the aspect-ratio check performed against a desktop
 * window shape.
 *
 * Extracted so the Settings preview swatches and the real field can share this one function, same
 * as the Android original — a hand-redrawn preview is a second copy that silently stops matching.
 */
fun DrawScope.drawCrystalMeshField(
    base: Color,
    blobs: List<Color>,
    dark: Boolean,
    /** The box the field's geometry is laid out in. Defaults to the drawn box. A popup passes the
     * screen here and its own screen position as [origin], so it shows the patch of field it
     * covers instead of a miniature of the whole thing. */
    fieldSize: Size = size,
    /** Where the drawn box sits inside [fieldSize]. */
    origin: Offset = Offset.Zero
) {
    drawRect(base)
    val r = BLOB_EXTENT
    val dilute = if (dark) MESH_BLOB_DILUTION_DARK else MESH_BLOB_DILUTION_LIGHT
    fun blob(color: Color, cx: Float, cy: Float, alpha: Float, rad: Float) {
        val center = Offset(cx - origin.x, cy - origin.y)
        val rx = fieldSize.width * rad
        val ry = fieldSize.height * rad
        if (rx <= 0f || ry <= 0f) return
        // Compose has no elliptical radial gradient: draw a circle of radius `ry` and squash it to
        // `rx` about its own centre, which keeps the gradient radial in the scaled space.
        scale(scaleX = rx / ry, scaleY = 1f, pivot = center) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha * dilute), Color.Transparent),
                    center = center,
                    radius = ry
                ),
                radius = ry,
                center = center
            )
        }
    }
    // Corner-anchored so the field reads as continuous rather than as five circles, with the centre
    // blob dimmed and shrunk so the middle of the screen stays the calmest part of it.
    blob(blobs[0], fieldSize.width * 0.02f, fieldSize.height * 0.04f, 0.85f, r)
    blob(blobs[1], fieldSize.width * 1.00f, fieldSize.height * 0.10f, 0.85f, r)
    blob(blobs[2], fieldSize.width * 0.50f, fieldSize.height * 0.42f, 0.64f, r * 0.9f)
    blob(blobs[3], fieldSize.width * 0.06f, fieldSize.height * 0.98f, 0.85f, r)
    blob(blobs[4], fieldSize.width * 0.98f, fieldSize.height * 1.00f, 0.85f, r)
}

/** What the screen field's blob alphas are multiplied by, per mode, so the mesh lands on the same
 * tone as the sheet panel gradient (ported separately, later phase). Dark is deliberately 1.0. */
const val MESH_BLOB_DILUTION_LIGHT = 0.68f
const val MESH_BLOB_DILUTION_DARK = 1.0f

/**
 * How far a blob reaches, as a fraction of **each** axis — an ellipse on a non-square surface, not
 * a circle. Per-axis is the whole point: a single scalar off the long edge was the exact bug
 * invariant #9 exists to prevent. See the Android original's own comment (`Crystal.kt`, the
 * `BLOB_EXTENT` declaration) for the full derivation of `1.14f`.
 */
private const val BLOB_EXTENT = 1.14f
