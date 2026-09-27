package com.apagon.rhythm.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Builds a `CrystalMesh.CUSTOM` field's five blob colours from one seed colour. Ported verbatim
 * from the Android original's `ui/theme/CrystalCustomMesh.kt` — pure LCh/CIELAB math, no platform
 * dependency. See that file (or the Stage 14 plan doc, invariant #8) for why the tones are fixed and
 * only hue/chroma vary, and why the third gamut-composite check is necessary and not sufficient on
 * its own.
 */

private const val CHANNEL_FLOOR = 19.0
private const val COMPOSITE_MARGIN = 0.50
private val HUE_OFFSETS = floatArrayOf(0f, 13f, 29f, 44f, 6f)
private val BLOB_ALPHA = floatArrayOf(0.85f, 0.85f, 0.64f, 0.85f, 0.85f)
private const val DILUTION_LIGHT = 0.68
private const val DILUTION_DARK = 1.0

private fun s2l(c: Double): Double {
    val v = c / 255.0
    return if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
}

private fun l2s(y: Double): Double {
    val s = if (y <= 0.0031308) 12.92 * y else 1.055 * Math.pow(y, 1 / 2.4) - 0.055
    return s * 255.0
}

private fun lab(r: Double, g: Double, b: Double): Triple<Double, Double, Double> {
    val rl = s2l(r); val gl = s2l(g); val bl = s2l(b)
    val x = 0.4124 * rl + 0.3576 * gl + 0.1805 * bl
    val y = 0.2126 * rl + 0.7152 * gl + 0.0722 * bl
    val z = 0.0193 * rl + 0.1192 * gl + 0.9505 * bl
    fun f(t: Double) = if (t > 0.008856) cbrt(t) else 7.787 * t + 16.0 / 116.0
    val fx = f(x / 0.95047); val fy = f(y); val fz = f(z / 1.08883)
    val a = 500 * (fx - fy)
    val bb = 200 * (fy - fz)
    return Triple(
        116 * fy - 16,
        hypot(a, bb),
        (Math.toDegrees(atan2(bb, a)) + 360.0) % 360.0
    )
}

private fun lchToRgb(l: Double, c: Double, h: Double): Pair<DoubleArray, Boolean> {
    val hr = Math.toRadians(h)
    val a = c * cos(hr)
    val b = c * sin(hr)
    val fy = (l + 16) / 116
    val fx = fy + a / 500
    val fz = fy - b / 200
    fun fi(t: Double) = if (t * t * t > 0.008856) t * t * t else (t - 16.0 / 116.0) / 7.787
    val x = fi(fx) * 0.95047; val y = fi(fy); val z = fi(fz) * 1.08883
    val vals = doubleArrayOf(
        l2s(3.2406 * x - 1.5372 * y - 0.4986 * z),
        l2s(-0.9689 * x + 1.8758 * y + 0.0415 * z),
        l2s(0.0557 * x - 0.2040 * y + 1.0570 * z)
    )
    return vals to vals.all { it >= -0.6 && it <= 255.6 }
}

private fun Color.rgbTriple(): DoubleArray {
    val argb = toArgb()
    return doubleArrayOf(
        ((argb shr 16) and 0xFF).toDouble(),
        ((argb shr 8) and 0xFF).toDouble(),
        (argb and 0xFF).toDouble()
    )
}

private fun compositedTone(rgb: DoubleArray, i: Int, base: DoubleArray, dark: Boolean): Double {
    val a = BLOB_ALPHA[i] * (if (dark) DILUTION_DARK else DILUTION_LIGHT)
    return lab(
        base[0] * (1 - a) + rgb[0] * a,
        base[1] * (1 - a) + rgb[1] * a,
        base[2] * (1 - a) + rgb[2] * a
    ).first
}

private fun solve(seedHue: Double, chromaScale: Float, dark: Boolean): List<Color> {
    val aurora = if (dark) AmbientBlobsDark else AmbientBlobsLight
    val base = (if (dark) AmbientBaseDark else AmbientBaseLight).rgbTriple()
    return (0 until 5).map { i ->
        val ref = aurora[i].rgbTriple()
        val (targetL, capC, _) = lab(ref[0], ref[1], ref[2])
        val refComposite = compositedTone(ref, i, base, dark)
        val hue = (seedHue + HUE_OFFSETS[i]) % 360.0
        var lo = 0.0
        var hi = capC * chromaScale
        repeat(40) {
            val mid = (lo + hi) / 2
            val (vals, inGamut) = lchToRgb(targetL, mid, hue)
            val ok = inGamut &&
                vals.min() >= CHANNEL_FLOOR - 0.5 &&
                abs(compositedTone(vals, i, base, dark) - refComposite) <= COMPOSITE_MARGIN
            if (ok) lo = mid else hi = mid
        }
        val (vals, _) = lchToRgb(targetL, lo, hue)
        Color(
            red = (vals[0].roundToInt().coerceIn(0, 255)) / 255f,
            green = (vals[1].roundToInt().coerceIn(0, 255)) / 255f,
            blue = (vals[2].roundToInt().coerceIn(0, 255)) / 255f
        )
    }
}

/** Memoised per `(hue, chromaScale, dark)` — the solve is ~400 iterations of cheap arithmetic, and
 * `RhythmTheme` re-reads it on every intensity drag frame once wired in. */
private val cache = HashMap<Triple<Int, Int, Boolean>, List<Color>>()

/**
 * The five blob colours for a custom field. [seed] is `null` until the user has picked one, in
 * which case this returns Aurora's own list.
 */
fun crystalCustomBlobs(seed: Color?, chromaScale: Float, dark: Boolean): List<Color> {
    if (seed == null) return if (dark) AmbientBlobsDark else AmbientBlobsLight
    val rgb = seed.rgbTriple()
    val (_, seedChroma, seedHue) = lab(rgb[0], rgb[1], rgb[2])
    val hue = if (seedChroma < 1.0) {
        val a = AmbientBlobsLight[0].rgbTriple()
        lab(a[0], a[1], a[2]).third
    } else seedHue
    val key = Triple(hue.roundToInt(), (chromaScale * 100).roundToInt(), dark)
    return cache.getOrPut(key) { solve(hue, chromaScale, dark) }
}
