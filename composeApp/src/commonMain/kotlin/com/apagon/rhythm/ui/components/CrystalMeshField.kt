package com.apagon.rhythm.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

// ── Borikén: the light-blue flag itself ──────────────────────────────────────────────────────────

/** The flag's colours: celeste (the 1895 light blue), white and red. Same values as `BorikenCombo`. */
private val FLAG_CELESTE = Color(0xFF3EA6F2)
private val FLAG_RED = Color(0xFFE4002B)

/**
 * The Borikén field as the flag (2026-10-13) — five red and white stripes, the celeste triangle at
 * the hoist and its white star — instead of the five-blob gradient, which read as Aurora. Softened
 * the way the mesh is: stripes feathered, colours below full strength and toned per mode, so the
 * glass panels over it still carry dark text in light mode and white text in dark mode. Same
 * [fieldSize]/[origin] convention as [drawCrystalMeshField]. A copy of the Android original's.
 */
fun DrawScope.drawBorikenFlagField(
    base: Color,
    dark: Boolean,
    fieldSize: Size = size,
    origin: Offset = Offset.Zero
) {
    drawRect(base)
    val w = fieldSize.width
    val h = fieldSize.height
    if (w <= 0f || h <= 0f) return
    val redAlpha = if (dark) 0.55f else 0.58f
    val whiteAlpha = if (dark) 0.16f else 0.0f // light mode: the base already is the white stripe
    val celesteAlpha = if (dark) 0.58f else 0.62f
    val starAlpha = if (dark) 0.72f else 0.92f

    // A tall screen (a phone held upright) gets the flag as it hangs vertically: triangle along the
    // top pointing down, stripes running top to bottom, star still upright. Laid flat across a
    // portrait screen the triangle became a long sliver and the star hid behind the cards.
    val vertical = h > w * 1.15f
    // Flag coordinates: `u` runs from the hoist along the stripes, `v` across them.
    val length = if (vertical) h else w
    val across = if (vertical) w else h
    fun at(u: Float, v: Float) =
        if (vertical) Offset(v - origin.x, u - origin.y) else Offset(u - origin.x, v - origin.y)

    // Stripes: red, white, red, white, red, each a fifth of the way across, edges feathered.
    val stripe = across / 5f
    val feather = 0.18f
    val edge = feather / (1f + 2f * feather)
    for (i in 0 until 5) {
        val color = if (i % 2 == 0) FLAG_RED.copy(alpha = redAlpha) else Color.White.copy(alpha = whiteAlpha)
        if (color.alpha == 0f) continue
        val from = i * stripe - stripe * feather
        val to = (i + 1) * stripe + stripe * feather
        val stops = arrayOf(
            0f to color.copy(alpha = if (i == 0) color.alpha else 0f),
            edge * 2f to color,
            1f - edge * 2f to color,
            1f to color.copy(alpha = if (i == 4) color.alpha else 0f)
        )
        val topLeft = at(0f, from)
        if (vertical) {
            drawRect(
                brush = Brush.horizontalGradient(*stops, startX = topLeft.x, endX = topLeft.x + (to - from)),
                topLeft = topLeft,
                size = Size(to - from, length)
            )
        } else {
            drawRect(
                brush = Brush.verticalGradient(*stops, startY = topLeft.y, endY = topLeft.y + (to - from)),
                topLeft = topLeft,
                size = Size(length, to - from)
            )
        }
    }

    // The triangle: equilateral on the hoist, as on the flag, but never more than ~60% of the
    // length so the stripes still read.
    val tip = minOf(across * 0.866f, length * 0.62f)
    val triangle = Path().apply {
        val a = at(0f, 0f); val b = at(tip, across / 2f); val c = at(0f, across)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close()
    }
    drawPath(triangle, FLAG_CELESTE.copy(alpha = celesteAlpha))

    // The star: five points, one straight up whichever way the flag hangs, at the triangle's
    // centroid, sized to the triangle.
    val centre = at(tip / 3f, across / 2f)
    val outer = minOf(tip, across) * 0.15f
    val inner = outer * 0.382f
    val star = Path().apply {
        for (k in 0 until 10) {
            val r = if (k % 2 == 0) outer else inner
            val ang = Math.toRadians(-90.0 + k * 36.0)
            val x = centre.x + (r * kotlin.math.cos(ang)).toFloat()
            val y = centre.y + (r * kotlin.math.sin(ang)).toFloat()
            if (k == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(star, Color.White.copy(alpha = starAlpha))
}
