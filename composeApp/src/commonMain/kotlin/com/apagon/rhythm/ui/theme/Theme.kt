package com.apagon.rhythm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.apagon.rhythm.data.preferences.CrystalBackground
import com.apagon.rhythm.data.preferences.CrystalMesh
import com.apagon.rhythm.data.preferences.CrystalStyle
import com.apagon.rhythm.data.preferences.ThemePreferences.Companion.DEFAULT_CRYSTAL_INTENSITY
import com.apagon.rhythm.data.preferences.ThemePreferences.Companion.DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA
import com.apagon.rhythm.data.preferences.ThemePreferences.Companion.CRYSTAL_BG_NEUTRAL
import com.apagon.rhythm.data.preferences.ThemeStyle
import com.materialkolor.rememberDynamicMaterialThemeState
import com.materialkolor.dynamicColorScheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import kotlin.math.abs
import kotlin.math.sign

// ── Crystal-only CompositionLocals (Stage 14 Phase 4) ───────────────────────────────────────────
// Ported from the Android original's Theme.kt. Provided in every style (arity must be constant —
// see RhythmTheme's own note) but only valued under Crystal.

/** The active theme style, so any composable can branch without it being threaded through every
 * call. Defaults to [ThemeStyle.MATERIAL3] — the same default the preference has. */
val LocalThemeStyle = staticCompositionLocalOf { ThemeStyle.MATERIAL3 }

/** The active Crystal panel intensity band (Sheer/Tinted). */
val LocalCrystalStyle = staticCompositionLocalOf { CrystalStyle.TINTED }

/** Where the Glass Intensity slider sits within the active preset's band, `0f..1f`. */
val LocalCrystalIntensity = staticCompositionLocalOf { DEFAULT_CRYSTAL_INTENSITY }

/** Whether Crystal draws its mesh or one flat cool neutral behind the panels. */
val LocalCrystalBackground = staticCompositionLocalOf { CrystalBackground.MESH }

/** Which of the ten shipped fields (or CUSTOM) the mesh draws. */
val LocalCrystalMesh = staticCompositionLocalOf { CrystalMesh.AURORA }

/** The seed a [CrystalMesh.CUSTOM] field is solved from; `null` means never configured. */
val LocalCrystalCustomSeed = staticCompositionLocalOf<Color?> { null }

/** How much of Aurora's chroma a [CrystalMesh.CUSTOM] field takes. */
val LocalCrystalCustomChroma = staticCompositionLocalOf { DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA }

/** The chosen Crystal room colour, or `null` for the cool neutral. See [crystalRoomColor]. */
val LocalCrystalRoom = staticCompositionLocalOf<Color?> { null }

/**
 * The seed colour the whole app's palette is built from.
 *
 * Normally the user's chosen accent. Under Crystal with a colour field, and only while the user has
 * never chosen an accent at all, the field supplies it instead. This is a *read*, not a write — an
 * earlier version of the Android original had the field picker call `setAccentColor` directly, which
 * repainted Material 3/Expressive too; deriving it here means nothing is ever overwritten.
 *
 * Both windows (desktop `main.kt`, Android `MainActivity.kt`) must call this rather than resolving
 * the accent themselves.
 */
fun accentSeed(
    themeStyle: ThemeStyle,
    crystalBackground: CrystalBackground,
    crystalMesh: CrystalMesh,
    accentColorIndex: Int,
    accentColorArgb: Int?,
    accentIsDefault: Boolean
): Color = if (
    themeStyle == ThemeStyle.CRYSTAL &&
    crystalBackground == CrystalBackground.MESH &&
    accentIsDefault
) {
    resolveDisplayColor(crystalMeshAccent(crystalMesh), null)
} else {
    resolveDisplayColor(accentColorIndex.coerceAtLeast(0), accentColorArgb)
}

// ── Crystal palette assembly ─────────────────────────────────────────────────────────────────────
// Crystal's neutrals are fixed (CrystalColor.kt); its accent comes from the user's chosen colour,
// derived here at Material 3's tonal positions via HCT rather than through materialkolor — same
// reason RhythmTheme pins SPEC_2021 for Material3: a library default must never silently restyle
// the app, and Crystal's palette is stated outright, in tone numbers, in one place.

private const val MIN_ACCENT_CHROMA = 32.0

private fun Color.atTone(tone: Double, minChroma: Double = MIN_ACCENT_CHROMA): Color {
    val hct = Hct.fromInt(toArgb())
    return Color(Hct.from(hct.hue, hct.chroma.coerceAtLeast(minChroma), tone).toInt())
}

/** The sibling of [atTone] for rooms/surfaces: *caps* chroma where [atTone] floors it — an accent
 * must stay saturated against neutral glass, a room must stay quiet behind everything. */
private fun Color.asRoomTone(tone: Double, maxChroma: Double): Color {
    val hct = Hct.fromInt(toArgb())
    return Color(Hct.from(hct.hue, hct.chroma.coerceAtMost(maxChroma), tone).toInt())
}

private const val ROOM_TONE_LIGHT = 88.0
private const val ROOM_CHROMA_LIGHT = 16.0
private const val ROOM_TONE_DARK = 11.0
private const val ROOM_CHROMA_DARK = 20.0

/**
 * The room colour, or `null` for the cool neutral. AMOLED wins over any tint — the toggle promises
 * true black.
 */
fun crystalRoomColor(index: Int, argb: Int?, dark: Boolean, amoled: Boolean): Color? {
    if (index == CRYSTAL_BG_NEUTRAL && argb == null) return null
    if (dark && amoled) return Color.Black
    val picked = resolveDisplayColor(index.coerceAtLeast(0), argb)
    return if (dark) picked.asRoomTone(ROOM_TONE_DARK, ROOM_CHROMA_DARK)
           else picked.asRoomTone(ROOM_TONE_LIGHT, ROOM_CHROMA_LIGHT)
}

private data class RoomSurfaces(
    val surface: Color, val surfaceVariant: Color, val surfaceBright: Color, val surfaceDim: Color,
    val containerLowest: Color, val containerLow: Color, val container: Color,
    val containerHigh: Color, val containerHighest: Color
)

private const val SURFACE_CHROMA = 6.0

private fun roomSurfaces(room: Color, dark: Boolean): RoomSurfaces = if (dark) {
    RoomSurfaces(
        surface = room.asRoomTone(18.0, SURFACE_CHROMA),
        surfaceVariant = room.asRoomTone(26.0, SURFACE_CHROMA),
        surfaceBright = room.asRoomTone(31.0, SURFACE_CHROMA),
        surfaceDim = room.asRoomTone(11.0, SURFACE_CHROMA),
        containerLowest = room.asRoomTone(10.0, SURFACE_CHROMA),
        containerLow = room.asRoomTone(15.0, SURFACE_CHROMA),
        container = room.asRoomTone(18.0, SURFACE_CHROMA),
        containerHigh = room.asRoomTone(23.0, SURFACE_CHROMA),
        containerHighest = room.asRoomTone(28.0, SURFACE_CHROMA)
    )
} else {
    RoomSurfaces(
        surface = room.asRoomTone(100.0, SURFACE_CHROMA),
        surfaceVariant = room.asRoomTone(93.0, SURFACE_CHROMA),
        surfaceBright = room.asRoomTone(100.0, SURFACE_CHROMA),
        surfaceDim = room.asRoomTone(86.0, SURFACE_CHROMA),
        containerLowest = room.asRoomTone(100.0, SURFACE_CHROMA),
        containerLow = room.asRoomTone(98.5, SURFACE_CHROMA),
        container = room.asRoomTone(96.0, SURFACE_CHROMA),
        containerHigh = room.asRoomTone(94.0, SURFACE_CHROMA),
        containerHighest = room.asRoomTone(92.0, SURFACE_CHROMA)
    )
}

/** Repaints a Crystal scheme's room and surfaces. A `copy()` on the finished neutral scheme, called
 * only when a colour is chosen — so the default look cannot drift by construction. */
private fun ColorScheme.withRoomSurfaces(room: Color, dark: Boolean): ColorScheme {
    val s = roomSurfaces(room, dark)
    return copy(
        background = room,
        surface = s.surface, surfaceVariant = s.surfaceVariant, surfaceBright = s.surfaceBright,
        surfaceDim = s.surfaceDim, surfaceContainerLowest = s.containerLowest,
        surfaceContainerLow = s.containerLow, surfaceContainer = s.container,
        surfaceContainerHigh = s.containerHigh, surfaceContainerHighest = s.containerHighest
    )
}

private fun crystalLight(seed: Color): ColorScheme = lightColorScheme(
    primary = seed.atTone(48.0), onPrimary = Color.White,
    primaryContainer = seed.atTone(90.0), onPrimaryContainer = seed.atTone(20.0),
    inversePrimary = seed.atTone(80.0),
    secondary = SlateSecondaryLight, onSecondary = Color.White,
    secondaryContainer = seed.atTone(93.0, minChroma = 8.0),
    onSecondaryContainer = seed.atTone(24.0, minChroma = 8.0),
    tertiary = seed.atTone(48.0), onTertiary = Color.White,
    tertiaryContainer = seed.atTone(90.0), onTertiaryContainer = seed.atTone(20.0),
    background = CoolWhite, onBackground = InkLight,
    surface = CoolSurfaceLight, onSurface = InkLight,
    surfaceVariant = CoolSurfaceVariantLight, onSurfaceVariant = InkMutedLight,
    surfaceTint = seed.atTone(48.0),
    surfaceDim = Color(0xFFDDDFE4), surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFF2F4F7), surfaceContainerHigh = Color(0xFFECEEF2),
    surfaceContainerHighest = Color(0xFFE5E8ED),
    outline = OutlineLight, outlineVariant = OutlineVariantLight,
    inverseSurface = Color(0xFF2F3033), inverseOnSurface = Color(0xFFF1F2F6)
)

/** Crystal, dark. `tertiary` sits two tonal steps above `primary`: solid fills use `primary`, but
 * accent text/icons on a translucent panel need `tertiary`'s lighter tone to stay legible. */
private fun crystalDark(seed: Color, amoled: Boolean): ColorScheme = darkColorScheme(
    primary = seed.atTone(58.0), onPrimary = Color.White,
    primaryContainer = seed.atTone(32.0), onPrimaryContainer = seed.atTone(92.0),
    inversePrimary = seed.atTone(45.0),
    secondary = SlateSecondaryDark, onSecondary = InkBackgroundDark,
    secondaryContainer = seed.atTone(28.0, minChroma = 8.0),
    onSecondaryContainer = seed.atTone(90.0, minChroma = 8.0),
    tertiary = seed.atTone(74.0), onTertiary = seed.atTone(18.0),
    tertiaryContainer = seed.atTone(32.0), onTertiaryContainer = seed.atTone(92.0),
    background = if (amoled) Color.Black else InkBackgroundDark, onBackground = InkDark,
    surface = CoolSurfaceDark, onSurface = InkDark,
    surfaceVariant = CoolSurfaceVariantDark, onSurfaceVariant = InkMutedDark,
    surfaceTint = seed.atTone(58.0),
    surfaceDim = if (amoled) Color.Black else InkBackgroundDark, surfaceBright = Color(0xFF38393C),
    surfaceContainerLowest = if (amoled) Color.Black else Color(0xFF060607),
    surfaceContainerLow = Color(0xFF141416), surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF26262A), surfaceContainerHighest = Color(0xFF313135),
    outline = OutlineDark, outlineVariant = OutlineVariantDark,
    inverseSurface = Color(0xFFE4E5EA), inverseOnSurface = Color(0xFF2F3033)
)

// ── Expressive hue containment (Stage 14 Phase 1, unchanged) ────────────────────────────────────

private const val EXPRESSIVE_MAX_HUE_DRIFT = 30.0
private const val HUE_MEANINGFUL_CHROMA = 5.0

private fun hueDelta(from: Double, to: Double): Double {
    var d = (to - from) % 360.0
    if (d > 180.0) d -= 360.0
    if (d <= -180.0) d += 360.0
    return d
}

private fun Color.clampHueTo(seedHue: Double): Color {
    val hct = Hct.fromInt(toArgb())
    if (hct.chroma < HUE_MEANINGFUL_CHROMA) return this
    val delta = hueDelta(seedHue, hct.hue)
    if (abs(delta) <= EXPRESSIVE_MAX_HUE_DRIFT) return this
    val pulled = seedHue + EXPRESSIVE_MAX_HUE_DRIFT * sign(delta)
    return Color(Hct.from((pulled % 360.0 + 360.0) % 360.0, hct.chroma, hct.tone).toInt())
}

private fun ColorScheme.withContainedHues(seed: Color): ColorScheme {
    val seedHue = Hct.fromInt(seed.toArgb()).hue
    return copy(
        secondary = secondary.clampHueTo(seedHue),
        onSecondary = onSecondary.clampHueTo(seedHue),
        secondaryContainer = secondaryContainer.clampHueTo(seedHue),
        onSecondaryContainer = onSecondaryContainer.clampHueTo(seedHue),
        inversePrimary = inversePrimary.clampHueTo(seedHue)
    )
}

// ── Crystal surface-alpha bands (invariant #4: opaque base, tintAlpha-only opacity) ─────────────
// Consumed both here (to pre-composite the ColorScheme) and by the surface-tier kit in
// ui/components/Crystal.kt (to compute the panel fill's own alpha) — the two must stay in the same
// units or a translucent role and a blurred panel disagree about how "converted" looks.

internal fun crystalSurfaceAlpha(style: CrystalStyle, dark: Boolean, intensity: Float): Float {
    val sheer = style == CrystalStyle.SHEER
    val low = when { sheer && dark -> 0.34f; sheer -> 0.28f; dark -> 0.58f; else -> 0.52f }
    val high = when { sheer && dark -> 0.66f; sheer -> 0.56f; dark -> 0.90f; else -> 0.84f }
    return low + (high - low) * intensity.coerceIn(0f, 1f)
}

/** Denser band for surfaces inside a window that paints its own field (a sheet/dialog) — its
 * backdrop is a smooth gradient, not the ambient mesh, so a screen-weight tint dissolves into it. */
internal fun crystalWindowSurfaceAlpha(style: CrystalStyle, dark: Boolean, intensity: Float): Float {
    val sheer = style == CrystalStyle.SHEER
    val low = when { sheer && dark -> 0.56f; sheer -> 0.52f; dark -> 0.70f; else -> 0.66f }
    val high = when { sheer && dark -> 0.92f; sheer -> 0.88f; dark -> 1.00f; else -> 0.98f }
    return low + (high - low) * intensity.coerceIn(0f, 1f)
}

/** How much surface colour lies over the room on a [CrystalBackground.SOLID] field — a different
 * band from [crystalSurfaceAlpha]/[crystalWindowSurfaceAlpha], tuned so the room reads through
 * rather than the panel going near-opaque (invariant #3's branch (b), pre-compositing). */
internal fun crystalSolidTint(style: CrystalStyle, dark: Boolean, intensity: Float): Float {
    val sheer = style == CrystalStyle.SHEER
    val low = when { sheer && dark -> 0.30f; sheer -> 0.26f; dark -> 0.42f; else -> 0.46f }
    val high = when { sheer && dark -> 0.52f; sheer -> 0.52f; dark -> 0.68f; else -> 0.78f }
    return low + (high - low) * intensity.coerceIn(0f, 1f)
}

private const val SOLID_SURFACE_LIFT_DARK = 10.0
private const val SOLID_SURFACE_LIFT_LIGHT = 2.4

private fun Color.liftedBy(tones: Double): Color {
    val hct = Hct.fromInt(toArgb())
    return Color(Hct.from(hct.hue, hct.chroma, (hct.tone + tones).coerceIn(0.0, 100.0)).toInt())
}

/** Puts alpha on every neutral surface role so the ambient field shows through existing surfaces
 * with no call-site edits. `background` is deliberately left opaque — it is the bottom of the
 * stack. */
internal fun ColorScheme.withTranslucentSurfaces(alpha: Float): ColorScheme = copy(
    surface = surface.copy(alpha = alpha), surfaceVariant = surfaceVariant.copy(alpha = alpha),
    surfaceBright = surfaceBright.copy(alpha = alpha), surfaceDim = surfaceDim.copy(alpha = alpha),
    surfaceContainerLowest = surfaceContainerLowest.copy(alpha = alpha),
    surfaceContainerLow = surfaceContainerLow.copy(alpha = alpha),
    surfaceContainer = surfaceContainer.copy(alpha = alpha),
    surfaceContainerHigh = surfaceContainerHigh.copy(alpha = alpha),
    surfaceContainerHighest = surfaceContainerHighest.copy(alpha = alpha)
)

/** The Solid counterpart: every surface role becomes the *opaque* colour a glass panel over the
 * flat field actually resolves to (invariant #3's branch (b) — no blur source, pre-composited, not
 * the alpha-compensation fallback branch (c) uses). Plain sRGB component mix (SrcOver), not a
 * perceptual `lerp`. */
internal fun ColorScheme.withSolidGlassSurfaces(field: Color, lift: Double, tint: Float): ColorScheme {
    fun glass(role: Color): Color {
        val over = role.liftedBy(lift)
        return Color(
            red = field.red * (1f - tint) + over.red * tint,
            green = field.green * (1f - tint) + over.green * tint,
            blue = field.blue * (1f - tint) + over.blue * tint,
            alpha = 1f
        )
    }
    return copy(
        surface = glass(surface), surfaceVariant = glass(surfaceVariant),
        surfaceBright = glass(surfaceBright), surfaceDim = glass(surfaceDim),
        surfaceContainerLowest = glass(surfaceContainerLowest),
        surfaceContainerLow = glass(surfaceContainerLow), surfaceContainer = glass(surfaceContainer),
        surfaceContainerHigh = glass(surfaceContainerHigh),
        surfaceContainerHighest = glass(surfaceContainerHighest)
    )
}

/**
 * The app's theme, in three styles. [ThemeStyle.MATERIAL3] is unchanged from before Stage 14 —
 * same palette engine, same spec version. All three read the user's accent colour, including
 * Crystal (fixed cool neutrals, accent roles hand-derived via HCT — invariant #6).
 */
@Composable
fun RhythmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    seedColor: Color = Color(0xFF6750A4),
    themeStyle: ThemeStyle = ThemeStyle.MATERIAL3,
    crystalStyle: CrystalStyle = CrystalStyle.TINTED,
    crystalIntensity: Float = DEFAULT_CRYSTAL_INTENSITY,
    crystalBackground: CrystalBackground = CrystalBackground.MESH,
    crystalMesh: CrystalMesh = CrystalMesh.AURORA,
    crystalMeshCustomSeed: Color? = null,
    crystalMeshCustomChroma: Float = DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA,
    crystalRoom: Color? = null,
    darkContrastLevel: Double = 0.0,
    content: @Composable () -> Unit
) {
    // See resolveTheme's own KDoc: this indirection exists so MaterialTheme (and therefore
    // `content`) is composed from exactly one call site regardless of style — invariant #1. Do not
    // reintroduce a branch that wraps `content`.
    val resolved = resolveTheme(
        darkTheme, isAmoled, seedColor, themeStyle, crystalStyle,
        crystalIntensity, crystalBackground, crystalRoom, darkContrastLevel
    )
    val crystal = themeStyle == ThemeStyle.CRYSTAL
    val inheritedContentColor = LocalContentColor.current
    MaterialTheme(
        colorScheme = resolved.colorScheme,
        typography = resolved.typography,
        shapes = resolved.shapes
    ) {
        CompositionLocalProvider(
            LocalThemeStyle provides themeStyle,
            LocalCrystalStyle provides crystalStyle,
            // Provided in every style but only valued under Crystal — arity must be constant.
            LocalCrystalIntensity provides if (crystal) crystalIntensity else LocalCrystalIntensity.current,
            LocalCrystalBackground provides if (crystal) crystalBackground else LocalCrystalBackground.current,
            LocalCrystalMesh provides if (crystal) crystalMesh else LocalCrystalMesh.current,
            LocalCrystalCustomSeed provides if (crystal) crystalMeshCustomSeed else LocalCrystalCustomSeed.current,
            LocalCrystalCustomChroma provides if (crystal) crystalMeshCustomChroma else LocalCrystalCustomChroma.current,
            LocalCrystalRoom provides if (crystal) crystalRoom else LocalCrystalRoom.current,
            // Crystal's panels are a Box + modifier, not a Material Surface, so nothing inside one
            // provides LocalContentColor and the fallback is Color.Black — invisible in dark Crystal.
            LocalContentColor provides if (crystal) resolved.colorScheme.onBackground else inheritedContentColor,
            content = content
        )
    }
}

private data class ResolvedTheme(
    val colorScheme: ColorScheme,
    val typography: Typography,
    val shapes: Shapes
)

/**
 * Picks the scheme/type/shapes triple for [themeStyle].
 *
 * Ported from the Android original: this function exists so [RhythmTheme] can compose `content`
 * from exactly one call site. Compose identifies a subtree by *where* it is called, so three
 * `if (…) { MaterialTheme { content() }; return }` blocks — one per style — each invoke `content`
 * from a different position, and switching style disposes and recreates the entire app below the
 * theme (the NavHost's back stack included). Branch on values here; compose once in [RhythmTheme].
 */
@Composable
private fun resolveTheme(
    darkTheme: Boolean,
    isAmoled: Boolean,
    seedColor: Color,
    themeStyle: ThemeStyle,
    crystalStyle: CrystalStyle,
    crystalIntensity: Float,
    crystalBackground: CrystalBackground,
    crystalRoom: Color?,
    darkContrastLevel: Double
): ResolvedTheme {
    // Crystal is not built on materialkolor, so it cannot go through DynamicMaterialTheme at all.
    if (themeStyle == ThemeStyle.CRYSTAL) {
        // Two remembers, not one: crystalIntensity changes every frame of a slider drag, and the
        // opaque scheme (~20 HCT conversions) depends on none of that.
        val opaque = remember(darkTheme, isAmoled, seedColor, crystalRoom) {
            val base = if (darkTheme) crystalDark(seedColor, isAmoled) else crystalLight(seedColor)
            if (crystalRoom != null) base.withRoomSurfaces(crystalRoom, darkTheme) else base
        }
        val colorScheme = remember(opaque, crystalStyle, crystalIntensity, crystalBackground) {
            if (crystalBackground == CrystalBackground.SOLID) {
                opaque.withSolidGlassSurfaces(
                    field = crystalRoom ?: if (darkTheme) opaque.background else CrystalSolidLight,
                    lift = if (darkTheme) SOLID_SURFACE_LIFT_DARK else SOLID_SURFACE_LIFT_LIGHT,
                    tint = crystalSolidTint(crystalStyle, darkTheme, crystalIntensity)
                )
            } else {
                opaque.withTranslucentSurfaces(crystalSurfaceAlpha(crystalStyle, darkTheme, crystalIntensity))
            }
        }
        return ResolvedTheme(colorScheme, Typography, CrystalShapes)
    }

    if (themeStyle == ThemeStyle.EXPRESSIVE) {
        val colorScheme = remember(darkTheme, isAmoled, seedColor, darkContrastLevel) {
            dynamicColorScheme(
                seedColor = seedColor,
                isDark = darkTheme,
                isAmoled = isAmoled,
                style = PaletteStyle.Vibrant,
                contrastLevel = if (darkTheme) darkContrastLevel else 0.0,
                specVersion = ColorSpec.SpecVersion.SPEC_2025
            ).withContainedHues(seedColor)
        }
        return ResolvedTheme(colorScheme, ExpressiveTypography, ExpressiveShapes)
    }

    // Material 3 / Crystal-fallback (unreachable now — kept as the else branch's shape).
    val material3State = rememberDynamicMaterialThemeState(
        seedColor = seedColor,
        isDark = darkTheme,
        isAmoled = isAmoled,
        contrastLevel = if (darkTheme) darkContrastLevel else 0.0,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2021
    )
    return ResolvedTheme(material3State.colorScheme, Typography, Material3Shapes)
}
