package com.apagon.rhythm.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath

/**
 * Reference: stitch_habit_ring_widget/ref/architectural_mindfulness/DESIGN.md ("Architectural
 * Mindfulness") — Forest Teal primary on Soft Mint surfaces, 28dp corner radius, never-black
 * teal-tinted shadows. The app's own dynamic accent color is used for "primary" everywhere
 * instead of the reference's literal #005050 — the reference teal is a proportions/usage example,
 * not a hardcoded brand color for this app.
 */
object WidgetColors {
    // Neutral text colors from the reference DESIGN.md front-matter (on-surface / on-surface-variant).
    val OnSurface = Color(0xFF191C1C)
    val OnSurfaceVariant = Color(0xFF3E4948)
    val OnPrimary = Color(0xFFFFFFFF)

    // Preset seeds matching habitColorPalette in Extensions.kt (index-matched)
    val PRESET_ARGBS = intArrayOf(
        0xFF6750A4.toInt(), // 0 Deep Purple (M3 default)
        0xFF006C4C.toInt(), // 1 Emerald
        0xFF0061A4.toInt(), // 2 Azure
        0xFFB3261E.toInt(), // 3 Ruby Red
        0xFFBC6000.toInt(), // 4 Amber
    )

    fun resolveAccent(accentIndex: Int, accentArgb: Int?): Int =
        accentArgb ?: PRESET_ARGBS.getOrElse(accentIndex) { PRESET_ARGBS[0] }

    fun surface(primaryArgb: Int, opacity: Float = 0.50f): Color = Color(primaryArgb).copy(alpha = opacity)
    fun border(primaryArgb: Int): Color = Color(primaryArgb).copy(alpha = 0.22f)

    // Flat near-white card — matches the reference DESIGN.md's `surface` token (#f8faf9) exactly.
    val DefaultCard = Color(0xFFF8FAF9)
    // Reference DESIGN.md `surface-mint` token — used for the App Theme tint style.
    val SurfaceMint = Color(0xFFE0F2F1)

    const val STYLE_DEFAULT = "default"
    const val STYLE_APP_THEME = "app_theme"
    const val STYLE_CUSTOM = "custom"
    /** Real wallpaper-derived Material You color — Android 12+ (API 31) only. */
    const val STYLE_SYSTEM = "system"

    /** Fixed sample palette for providePreview() (widget-picker gallery) — never varies by device. */
    val defaultPreviewPalette: Palette
        get() = Palette(
            cardBackground = DefaultCard,
            primaryArgb = PRESET_ARGBS[0],
            onSurfaceArgb = OnSurface.toArgb(),
            onSurfaceVariantArgb = OnSurfaceVariant.toArgb()
        )

    fun systemStyleAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Foreground accent (ring/text/icons) — only "custom" style overrides the app's real accent color. */
    fun resolveForeground(style: String?, customArgb: Int?, appAccentArgb: Int): Int =
        if (style == STYLE_CUSTOM) (customArgb ?: appAccentArgb) else appAccentArgb

    /** Card background fill for the given style (all styles except "system" — see [resolvePalette]). */
    fun cardBackground(style: String?, customArgb: Int?, appAccentArgb: Int, opacity: Float): Color =
        when (style) {
            STYLE_APP_THEME -> surface(appAccentArgb, opacity)
            STYLE_CUSTOM -> surface(customArgb ?: appAccentArgb, opacity)
            else -> DefaultCard
        }

    /**
     * Every color a widget card needs, resolved once per [provideGlance] call. For
     * [STYLE_SYSTEM] on Android 12+, all four values come from the platform's real
     * wallpaper-derived Material You scheme (`dynamicLightColorScheme`/`dynamicDarkColorScheme`)
     * instead of the app's own accent — this is what makes a widget "look like Google made it."
     * Below API 31, "system" silently falls back to the same resolution as [STYLE_DEFAULT].
     */
    data class Palette(
        val cardBackground: Color,
        val primaryArgb: Int,
        val onSurfaceArgb: Int,
        val onSurfaceVariantArgb: Int
    )

    fun resolvePalette(
        context: Context,
        style: String?,
        customArgb: Int?,
        appAccentArgb: Int,
        opacity: Float
    ): Palette {
        if (style == STYLE_SYSTEM && systemStyleAvailable()) {
            val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            return Palette(
                cardBackground = scheme.surfaceContainerHigh,
                primaryArgb = scheme.primary.toArgb(),
                onSurfaceArgb = scheme.onSurface.toArgb(),
                onSurfaceVariantArgb = scheme.onSurfaceVariant.toArgb()
            )
        }
        return Palette(
            cardBackground = cardBackground(style, customArgb, appAccentArgb, opacity),
            primaryArgb = resolveForeground(style, customArgb, appAccentArgb),
            onSurfaceArgb = OnSurface.toArgb(),
            onSurfaceVariantArgb = OnSurfaceVariant.toArgb()
        )
    }

    /** Per-day dot color for the Streak Spotlight current-month calendar. `null` cell = blank grid padding (invisible). */
    internal fun calendarDotColor(state: DayState?, primaryArgb: Int, onSurfaceVariantArgb: Int): Color = when (state) {
        DayState.DONE -> Color(primaryArgb)
        DayState.MISSED -> border(primaryArgb)
        DayState.NOT_SCHEDULED -> Color(onSurfaceVariantArgb).copy(alpha = 0.15f)
        DayState.NO_DATA -> border(primaryArgb).copy(alpha = 0.10f)
        null -> Color.Transparent
    }

    fun progressBitmap(progress: Float, sizePx: Int, primaryArgb: Int = PRESET_ARGBS[0]): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = sizePx * 0.12f
        val inset = stroke / 2f + 4f
        val oval = RectF(inset, inset, sizePx - inset, sizePx - inset)

        val trackAlpha = (0.20f * 255).toInt()
        val trackColor = android.graphics.Color.argb(
            trackAlpha,
            android.graphics.Color.red(primaryArgb),
            android.graphics.Color.green(primaryArgb),
            android.graphics.Color.blue(primaryArgb)
        )
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(oval, -90f, 360f, false, trackPaint)

        if (progress > 0f) {
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryArgb
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(oval, -90f, progress.coerceIn(0f, 1f) * 360f, false, arcPaint)
        }

        return bitmap
    }

    /**
     * Rasterizes a Compose [ImageVector] (e.g. from habitIconLibrary) into a tinted [Bitmap],
     * for use in Glance widgets where arbitrary ImageVectors can't be rendered directly.
     * Walks the vector's VectorGroup/VectorPath tree the same way the TriathlonIcon composite
     * technique does, drawing each path onto a plain Canvas — no Composable context needed.
     */
    fun iconToBitmap(vector: ImageVector, sizePx: Int, tintArgb: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tintArgb
            style = Paint.Style.FILL
        }
        val scale = sizePx / vector.viewportWidth
        canvas.scale(scale, scale)
        drawVectorGroup(vector.root, canvas, paint)
        return bitmap
    }

    private fun drawVectorGroup(group: VectorGroup, canvas: Canvas, paint: Paint) {
        canvas.save()
        canvas.translate(group.translationX, group.translationY)
        canvas.translate(group.pivotX, group.pivotY)
        canvas.rotate(group.rotation)
        canvas.scale(group.scaleX, group.scaleY)
        canvas.translate(-group.pivotX, -group.pivotY)
        for (node in group) {
            when (node) {
                is VectorPath -> {
                    val path = PathParser().addPathNodes(node.pathData).toPath().asAndroidPath()
                    canvas.drawPath(path, paint)
                }
                is VectorGroup -> drawVectorGroup(node, canvas, paint)
            }
        }
        canvas.restore()
    }

    /**
     * Rasterizes bold text using a real bundled typeface (Glance's TextStyle can't resolve the
     * app's downloadable-Google-Fonts Plus Jakarta Sans, so the highest-visibility "hero" numbers
     * — Habit Ring's count, Streak's streak number, Timer's MM:SS — use this instead of a plain
     * Glance Text() with the system font).
     */
    fun textToBitmap(text: String, widthPx: Int, heightPx: Int, textSizePx: Float, typeface: Typeface, tintArgb: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tintArgb
            this.typeface = typeface
            textSize = textSizePx
            textAlign = Paint.Align.CENTER
        }
        val metrics = paint.fontMetrics
        val baselineY = heightPx / 2f - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(text, widthPx / 2f, baselineY, paint)
        return bitmap
    }
}
