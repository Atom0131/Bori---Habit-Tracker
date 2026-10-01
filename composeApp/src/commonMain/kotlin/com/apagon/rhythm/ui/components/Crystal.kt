package com.apagon.rhythm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.preferences.CrystalBackground
import com.apagon.rhythm.data.preferences.CrystalMesh
import com.apagon.rhythm.data.preferences.CrystalStyle
import com.apagon.rhythm.data.preferences.ThemeStyle
import com.apagon.rhythm.platform.GlassBlur
import com.apagon.rhythm.platform.GlassBlurField
import com.apagon.rhythm.ui.theme.AmbientBaseDark
import com.apagon.rhythm.ui.theme.AmbientBaseLight
import com.apagon.rhythm.ui.theme.CrystalSolidLight
import com.apagon.rhythm.ui.theme.LocalCrystalBackground
import com.apagon.rhythm.ui.theme.LocalCrystalIntensity
import com.apagon.rhythm.ui.theme.LocalCrystalMesh
import com.apagon.rhythm.ui.theme.LocalCrystalRoom
import com.apagon.rhythm.ui.theme.LocalCrystalStyle
import com.apagon.rhythm.ui.theme.LocalThemeStyle
import com.apagon.rhythm.ui.theme.crystalFieldBlobs
import com.apagon.rhythm.ui.theme.crystalSurfaceAlpha
import com.apagon.rhythm.ui.theme.crystalWindowSurfaceAlpha
import com.apagon.rhythm.ui.theme.withTranslucentSurfaces
import org.koin.compose.koinInject

// =================================================================================================
// The Crystal component kit — Stage 14 Phase 4 port. See
// ref_notes/plan_2026-09-26_stage14_crystal_theme_port.md for the full invariant inventory this
// file exists to preserve; comments below cite invariant numbers from that doc.
//
// **Ported subset, stated up front rather than left implicit**: the 8 surface tiers
// (invariant #5), CrystalRoot's window/field model (invariant #2, adapted below), the
// blur/Solid/fallback three-way split (invariant #3, via GlassBlur), and the opaque-base/
// tintAlpha-only opacity rule (invariant #4). NOT ported this pass: the long tail of individual
// Material-component colour overrides (switch/checkbox/radio/slider/chip/textField/button/
// listItem colours, ~15 functions in the Android original) and CrystalNavIndicator/
// crystalNavItemColors/CrystalBar's nav-selection styling — these are real, cited in CLAUDE.md's
// "Selection is chrome too" section as load-bearing, but are component-by-component recolouring
// on top of the surface kit, not the kit itself; deferred as a named follow-up, not silently
// dropped. `CrystalDiag.kt` (Android-only, temporary debug logger) is deliberately not ported.
//
// **The one deliberate desktop simplification**: the Android original's [CrystalWindowContent]/
// `crystalMenuField` paint the field at absolute *screen* pixel coordinates (`positionOnScreen()` +
// `displayMetrics`) so a popup shows the exact patch of field behind it, matching the main screen
// continuously. That machinery assumes Android's multi-window-over-one-screen model. Desktop
// windows are typically singular and don't overlap a visible "rest of the app" the same way, so
// this port draws the window's own field at its *local* bounds instead of screen-absolute ones —
// preserving the invariant that actually matters (a fresh, non-null field per window, replacing
// not severing the ambient one) without the screen-position plumbing. Flagged, not hidden.
// =================================================================================================

/** True when the Crystal theme is active. The kit's single branch point. */
@Composable
@ReadOnlyComposable
fun isCrystal(): Boolean = LocalThemeStyle.current == ThemeStyle.CRYSTAL

internal val isDark: Boolean
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

private val isSheer: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalCrystalStyle.current == CrystalStyle.SHEER

/** True inside a window that paints its own field (a sheet/dialog) rather than the app's ambient
 * one — panels there take the denser [crystalWindowSurfaceAlpha] band. */
val LocalCrystalInWindow = staticCompositionLocalOf { false }
private val inOwnWindow: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalCrystalInWindow.current

/** True when Crystal draws one flat colour rather than the five-colour mesh (invariant #3: no blur
 * source is registered at all under Solid — blurring a flat colour returns that flat colour). */
private val isSolidField: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalCrystalBackground.current == CrystalBackground.SOLID

@Composable
@ReadOnlyComposable
private fun solidFieldColor(): Color =
    LocalCrystalRoom.current ?: if (isDark) MaterialTheme.colorScheme.background else CrystalSolidLight

/** The container colour a Scaffold should use — transparent under Crystal so the ambient field
 * shows through; the normal background otherwise. */
@Composable
@ReadOnlyComposable
fun crystalScaffoldColor(fallback: Color = Color.Unspecified): Color = when {
    isCrystal() -> Color.Transparent
    fallback.isSpecified -> fallback
    else -> MaterialTheme.colorScheme.background
}

/** Pairs with [crystalScaffoldColor]. Must be passed explicitly: `contentColorFor(Transparent)` is
 * `Color.Unspecified`, which would publish as `LocalContentColor` and render every unstyled `Text`
 * black — invisible in dark Crystal. */
@Composable
@ReadOnlyComposable
fun crystalScaffoldContentColor(): Color = MaterialTheme.colorScheme.onBackground

/** Stage 17b: every screen's `TopAppBar` was passing no `colors =` at all, so it rendered as a
 * flat opaque Material bar (near-white) even under Crystal — the "white square" every screen
 * showed at the top. Transparent container so the ambient field shows through, same as
 * [crystalScaffoldColor]; content colours track the same roles a themed Scaffold already uses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun crystalTopAppBarColors(): TopAppBarColors = if (isCrystal()) {
    TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        actionIconContentColor = MaterialTheme.colorScheme.onBackground
    )
} else {
    TopAppBarDefaults.topAppBarColors()
}

// ── The tiers (invariant #5: crystalSurface is private, exactly these 8, fill is the only open
// parameter) ──────────────────────────────────────────────────────────────────────────────────────

private val SECTION_ELEVATION = 4.dp
private val CARD_ELEVATION = 3.dp
private val SMALL_OBJECT_ELEVATION = 2.dp

@Composable
fun Modifier.crystalStickyHeader(
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 4.dp
): Modifier = if (isCrystal()) {
    this.crystalSurface(
        shape = MaterialTheme.shapes.large,
        fill = MaterialTheme.colorScheme.surfaceContainer,
        crystalElevation = SECTION_ELEVATION,
        stickyFallbackBoost = true
    )
} else {
    this.background(MaterialTheme.colorScheme.background)
}

@Composable
fun Modifier.crystalCardSurface(
    fill: Color = MaterialTheme.colorScheme.surfaceContainer
): Modifier = this.crystalSurface(shape = MaterialTheme.shapes.large, fill = fill, crystalElevation = CARD_ELEVATION)

/** [shape] defaults to the tier's own `small` radius, but a date/time picker trigger row sitting
 * directly under a large-radius text field (e.g. the New Event sheet) needs to match that field's
 * shape, not the tier default, to avoid the same "boxy next to pill" clash `crystalTextFieldShape()`
 * fixes for text fields — pass `MaterialTheme.shapes.large` there. */
@Composable
fun Modifier.crystalControlSurface(
    fill: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    shape: Shape = MaterialTheme.shapes.small
): Modifier {
    return this
        .crystalSurface(shape = shape, fill = fill, elevation = SMALL_OBJECT_ELEVATION, crystalElevation = SMALL_OBJECT_ELEVATION)
        .then(
            if (isCrystal()) Modifier
            else Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape)
        )
}

@Composable
fun Modifier.crystalTileSurface(
    fill: Color = MaterialTheme.colorScheme.surfaceContainer
): Modifier = this.crystalSurface(shape = MaterialTheme.shapes.medium, fill = fill, crystalElevation = SMALL_OBJECT_ELEVATION)

@Composable
fun Modifier.crystalChipSurface(fill: Color): Modifier =
    this.crystalSurface(shape = CircleShape, fill = fill, crystalElevation = SMALL_OBJECT_ELEVATION)

@Composable
fun Modifier.crystalIconButtonSurface(): Modifier = if (isCrystal()) {
    this.crystalSurface(shape = CircleShape, fill = MaterialTheme.colorScheme.surfaceContainerHighest, elevation = 0.dp, crystalElevation = 1.dp)
} else this

/** Stage 17f: every glyph-only action in this app (search "⌕", overflow "⋮", lock "🔓"/"🔒", close
 * "×") was a bare `TextButton { Text(glyph) }` — `TextButton`'s content padding is sized for text,
 * not a single centered glyph, so these all read as cramped/off-center. A fixed-size circular
 * touch target with the glyph centered inside it, using the already-built (but until now unused)
 * [crystalIconButtonSurface] for the Crystal-only glass fill. */
@Composable
fun CrystalIconButton(glyph: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .crystalIconButtonSurface()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = glyph, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun Modifier.crystalFabSurface(): Modifier = if (isCrystal()) {
    this.crystalSurface(shape = CircleShape, fill = MaterialTheme.colorScheme.surfaceContainerHighest, elevation = 0.dp, crystalElevation = 3.dp)
} else this

@Composable
fun Modifier.crystalChromeSurface(
    fill: Color = MaterialTheme.colorScheme.surfaceContainer
): Modifier = this.crystalSurface(shape = RectangleShape, fill = fill, crystalElevation = 0.dp, rim = false)

/** The FAB's shadow under Crystal: none, on every background including Solid — `primary` is an
 * accent role, never pre-composited by `withSolidGlassSurfaces`, so it stays translucent there too
 * and needs the same zeroed native shadow [crystalControlElevation] gives neutral-role controls. */
@Composable
fun crystalFabElevation(): FloatingActionButtonElevation = if (isCrystal()) {
    FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
} else {
    FloatingActionButtonDefaults.elevation()
}

@Composable
fun crystalFabContainerColor(): Color = if (isCrystal()) Color.Transparent else MaterialTheme.colorScheme.primary

@Composable
fun crystalFabContentColor(): Color = if (isCrystal()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary

/** A tint only — no blur, no rim. For accent-container roles (never touched by
 * `withTranslucentSurfaces`/`withSolidGlassSurfaces`, so permanently opaque otherwise) and small
 * decorative shapes too small for a rim/shadow to read as glass rather than noise. */
@Composable
@ReadOnlyComposable
fun crystalControlColor(fallback: Color): Color = when {
    !isCrystal() -> fallback
    isSolidField -> fallback
    else -> fallback.copy(alpha = fallback.alpha * 0.5f)
}

@Composable
@ReadOnlyComposable
fun crystalControlElevation(fallback: Dp): Dp = if (isCrystal() && !isSolidField) 0.dp else fallback

@Composable
@ReadOnlyComposable
fun crystalSelectedChipColor(fallback: Color): Color =
    if (isCrystal()) MaterialTheme.colorScheme.surfaceContainerHighest else fallback

@Composable
@ReadOnlyComposable
fun crystalSelectedChipContentColor(fallback: Color): Color =
    if (isCrystal()) MaterialTheme.colorScheme.primary else fallback

@Composable
@ReadOnlyComposable
fun crystalAddTileFill(): Color =
    if (isCrystal()) MaterialTheme.colorScheme.surfaceContainerHighest
    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

// ── Stock Material control colours — ported from the Android original's Crystal.kt ──────────────

/** A `Switch`'s colours: glass track when unchecked, accent when checked. */
@Composable
fun crystalSwitchColors(): SwitchColors = if (isCrystal()) {
    SwitchDefaults.colors(
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedTrackColor = crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest),
        uncheckedBorderColor = MaterialTheme.colorScheme.outline
    )
} else {
    SwitchDefaults.colors()
}

/** A `Checkbox`'s colours: glass box when unchecked, accent when checked. */
@Composable
fun crystalCheckboxColors(): CheckboxColors = if (isCrystal()) {
    CheckboxDefaults.colors(
        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
        checkedColor = MaterialTheme.colorScheme.primary,
        checkmarkColor = MaterialTheme.colorScheme.onPrimary
    )
} else {
    CheckboxDefaults.colors()
}

/** A `RadioButton`'s colours. Strokes rather than a fill, so this only needs the accent pinned. */
@Composable
fun crystalRadioButtonColors(): RadioButtonColors = if (isCrystal()) {
    RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colorScheme.primary,
        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
} else {
    RadioButtonDefaults.colors()
}

/**
 * A `Slider`'s colours. The inactive track is the part that matters — left at Material's default
 * it is the flattest object on a Crystal settings screen.
 */
@Composable
fun crystalSliderColors(): SliderColors = if (isCrystal()) {
    SliderDefaults.colors(
        inactiveTrackColor = crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest)
    )
} else {
    SliderDefaults.colors()
}

/**
 * An `OutlinedTextField`'s colours. The container goes to glass and the indicator follows the accent.
 * Ported from the Android original for the handful of real `OutlinedTextField`s in this desktop port
 * (the habit-entry field, the sync peer-address field) that would otherwise stay stock Material.
 */
@Composable
fun crystalTextFieldColors(): TextFieldColors = if (isCrystal()) {
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest),
        unfocusedContainerColor = crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )
} else {
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * An `OutlinedTextField`'s colours for the case [crystalTextFieldColors] doesn't cover: a field
 * that already sits inside another `crystalCardSurface()`/`crystalTileSurface()` row (an inline
 * "type here, then tap this button" row). Giving it its own glass fill *and* border there paints a
 * second, smaller box nested inside the row's box — under Crystal this goes fully transparent, so
 * the field reads as plain text sitting on the parent's glass, the closest this desktop port gets
 * to the Android original's `FluidTextField` (a glass box around a transparent field) without
 * porting that whole composable.
 */
@Composable
fun crystalBareTextFieldColors(): TextFieldColors = if (isCrystal()) {
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )
} else {
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * An `OutlinedTextField`'s shape, to go with [crystalTextFieldColors]. `TextFieldColors` has no
 * shape slot, and a plain `OutlinedTextField` does **not** pick up `MaterialTheme.shapes` the way
 * stock Material surfaces do — its own default is the much tighter `extraSmall`, which next to this
 * kit's `large`-radius cards (`crystalCardSurface()`) reads as a plain boxy rectangle instead of the
 * soft, almost-pill glass field the Android original uses. Large, to match. Under the two Material
 * styles this returns `medium` rather than the stock default too — several call sites already hand-set
 * `RoundedCornerShape(16.dp)` for those (Material3's `medium` token, exactly), so routing through here
 * instead keeps that look while making it theme-aware rather than a hardcoded literal.
 */
@Composable
fun crystalTextFieldShape(): Shape =
    if (isCrystal()) MaterialTheme.shapes.large else MaterialTheme.shapes.medium

/**
 * A filled `Button`'s colours — **transparent under Crystal**, because the glass is already painted
 * by the caller's surrounding `crystalCardSurface()`/`crystalControlSurface()` and the accent moves
 * to the label instead of painting a second, opaque slab on top of the glass. Ported from the
 * Android original.
 */
@Composable
fun crystalButtonColors(): ButtonColors = if (isCrystal()) {
    ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
} else {
    ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** A glyph etched into the glass — a dark+light offset pair behind the tinted icon, the bevel a
 * real piece of glass has. Under the two Material styles, a plain `Icon`. */
@Composable
fun CrystalGlyph(
    imageVector: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String? = null
) {
    if (!isCrystal()) {
        Icon(imageVector, contentDescription, modifier.size(size), tint = tint)
        return
    }
    val bevel = 0.75.dp
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(imageVector, null, Modifier.size(size).offset(x = bevel, y = bevel), tint = Color.Black.copy(alpha = 0.30f))
        Icon(imageVector, null, Modifier.size(size).offset(x = -bevel, y = -bevel), tint = Color.White.copy(alpha = 0.34f))
        Icon(imageVector, contentDescription, Modifier.size(size), tint = tint)
    }
}

// ── Panel gradient (sheet/dialog fallback base colour) ───────────────────────────────────────────

private const val PANEL_LERP_LIGHT = 0.45f
private const val PANEL_LERP_DARK = 0.82f
private val PANEL_STOP_SHAPE = floatArrayOf(0.85f, 0.85f, 0.64f, 0.85f, 0.85f)
    .let { shape -> val mean = shape.average().toFloat(); shape.map { it / mean } }

@Composable
@ReadOnlyComposable
private fun panelFieldStops(mesh: CrystalMesh, dark: Boolean): List<Color> {
    val blobs = crystalFieldBlobs(mesh, dark)
    val base = if (dark) AmbientBaseDark else AmbientBaseLight
    val lerpMean = if (dark) PANEL_LERP_DARK else PANEL_LERP_LIGHT
    return blobs.mapIndexed { i, blob ->
        val t = (lerpMean * PANEL_STOP_SHAPE.getOrElse(i) { 1f }).coerceIn(0f, 1f)
        lerp(base, blob, t)
    }
}

/** The base colour of a window that paints its own field (a sheet/dialog), for chrome outside the
 * content slot that can't reach a drawn field directly (a drag handle strip). */
@Composable
@ReadOnlyComposable
fun crystalSheetColor(fallback: Color = Color.Unspecified): Color = when {
    isCrystal() && isSolidField -> solidFieldColor()
    isCrystal() -> panelFieldStops(LocalCrystalMesh.current, isDark).first()
    fallback.isSpecified -> fallback
    else -> MaterialTheme.colorScheme.surfaceContainerLow
}

/** The rim — a soft highlight describing a panel's edge, not a sectioning line. Flat rather than a
 * diagonal gradient: this app puts many panels on screen at once, and a repeated directional
 * highlight reads as a pattern. */
@Composable
@ReadOnlyComposable
private fun rimColor(): Color = Color.White.copy(alpha = if (isDark) 0.30f else 0.45f)

/** The translucent-fill alpha used when there is no blur field in scope at all (Solid excluded —
 * that path is pre-composited opaque, see [crystalSurface]). Same bands `Theme.kt` uses for the
 * translucent surface *roles*, so an untouched `Surface()` and a converted panel agree. */
@Composable
@ReadOnlyComposable
private fun fauxFillAlpha(): Float {
    val t = LocalCrystalIntensity.current
    return if (inOwnWindow) crystalWindowSurfaceAlpha(LocalCrystalStyle.current, isDark, t)
    else crystalSurfaceAlpha(LocalCrystalStyle.current, isDark, t)
}

/** Tint alpha + blur radius for the active Sheer/Tinted band — pure Kotlin, no platform check.
 * Which of these two actually reaches a real blur vs. a translucent fallback is decided by
 * [GlassBlur] per platform, not here (invariant #3, collapsed to what the DI interface already
 * models — see this file's header comment). */
private fun band(low: Float, high: Float, t: Float): Float = low + (high - low) * t.coerceIn(0f, 1f)

@Composable
@ReadOnlyComposable
private fun crystalTintAlpha(): Float {
    val sheer = isSheer; val dark = isDark; val t = LocalCrystalIntensity.current
    return when {
        sheer && dark -> band(0.08f, 0.24f, t)
        sheer -> band(0.04f, 0.16f, t)
        dark -> band(0.16f, 0.36f, t)
        else -> band(0.14f, 0.30f, t)
    }
}

@Composable
@ReadOnlyComposable
private fun crystalBlurRadius(): Dp {
    val t = LocalCrystalIntensity.current.coerceIn(0f, 1f)
    val (lowDp, highDp) = if (isSheer) 4f to 16f else 8f to 24f
    return (lowDp + (highDp - lowDp) * t).dp
}

/** The ambient colour field. Fills its bounds; place as the lowest layer of the region marked with
 * the current [GlassBlur]'s source modifier. Under the two Material styles, the solid background. */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    if (!isCrystal()) {
        Box(modifier.background(MaterialTheme.colorScheme.background)); return
    }
    if (isSolidField) {
        Box(modifier.background(solidFieldColor())); return
    }
    val dark = isDark
    val mesh = LocalCrystalMesh.current
    val base = if (dark) AmbientBaseDark else AmbientBaseLight
    val blobs = crystalFieldBlobs(mesh, dark)
    Box(modifier.drawBehind { drawCrystalMeshField(base, blobs, dark) })
}

/**
 * Crystal panel styling as a [Modifier] — the workhorse of this file. Under Crystal: a real
 * backdrop blur when a field is in scope (via [GlassBlur]), a translucent/pre-composited fill
 * otherwise, then the rim and a floating shadow. Under the two Material styles: clip + fill +
 * elevation, unchanged from before Stage 14.
 *
 * **Private, and must stay that way (invariant #5).** A caller may say *what colour*; shape,
 * elevation and rim are the closed vocabulary of the 8 tiers above it.
 */
@Composable
private fun Modifier.crystalSurface(
    shape: Shape = MaterialTheme.shapes.large,
    fill: Color = MaterialTheme.colorScheme.surfaceContainer,
    elevation: Dp = 0.dp,
    crystalElevation: Dp = 3.dp,
    rim: Boolean = true,
    stickyFallbackBoost: Boolean = false
): Modifier {
    if (!isCrystal()) {
        return this
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false) else Modifier)
            .clip(shape)
            .background(fill)
    }
    val glassBlur = koinInject<GlassBlur>()
    val field = LocalGlassBlurField.current
    val fillMod = if (isSolidField) {
        // Invariant #3 branch (b): `fill` is already the exact opaque colour
        // `withSolidGlassSurfaces` pre-composited — drawing it at any alpha would composite the
        // field into itself a second time.
        Modifier.background(fill.copy(alpha = 1f))
    } else {
        val alpha = if (stickyFallbackBoost) 0.96f else fauxFillAlpha()
        Modifier.then(
            glassBlur.effectModifier(
                field = field,
                tintColor = fill.copy(alpha = crystalTintAlpha()),
                fallbackAlpha = alpha,
                blurRadius = crystalBlurRadius(),
                modifier = Modifier
            )
        )
    }
    return this
        .shadow(crystalElevation, shape, clip = false)
        .clip(shape)
        .then(fillMod)
        .then(if (rim) Modifier.border(1.dp, rimColor(), shape) else Modifier)
}

/** A Crystal panel as a container, for callers that would otherwise need a bare wrapper `Box`
 * anyway. Anchors `LocalContentColor` so text inside stays legible in both modes. */
@Composable
fun CrystalSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    fill: Color = MaterialTheme.colorScheme.surfaceContainer,
    elevation: Dp = 0.dp,
    crystalElevation: Dp = 6.dp,
    rim: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.crystalSurface(shape, fill, elevation, crystalElevation, rim)) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content() }
    }
}

/** The blur field in scope, provided by [CrystalRoot] or [CrystalWindowContent]. `null` means "no
 * source" — a panel falls back to a translucent/pre-composited fill, correct and deliberate inside
 * a bottom sheet or dialog before it installs its own. */
val LocalGlassBlurField = staticCompositionLocalOf<GlassBlurField?> { null }

/**
 * Installs the ambient field for one window (invariant #2: **once per window**, hoisted above the
 * app's NavHost so the field stays fixed while tabs cross-fade). Under the two Material styles, a
 * pass-through with no extra layout node.
 *
 * Composes `content` from exactly one call site regardless of style (invariant #1's sibling fix —
 * the `AmbientBackground` sibling appearing/disappearing is safe, since Compose keys siblings by
 * call site, not index, so `content`'s identity never moves with it).
 */
@Composable
fun CrystalRoot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val crystal = isCrystal()
    val glassBlur = koinInject<GlassBlur>()
    val field = if (crystal && !isSolidField) glassBlur.rememberField() else null
    Box(modifier.fillMaxSize()) {
        if (crystal) {
            AmbientBackground(
                Modifier.fillMaxSize().let { if (field != null) glassBlur.sourceModifier(field, it) else it }
            )
        }
        CompositionLocalProvider(LocalGlassBlurField provides field, content = content)
    }
}

/**
 * A Crystal root for a sub-composition in **its own window** — a bottom sheet, a dialog, a popup
 * (invariant #2). The inherited field from [CrystalRoot] is unusable across windows and must be
 * *replaced*, not merely severed — severing alone (providing `null` and stopping there) was the
 * bug this port already shipped once tonight: every panel fell back to a flat near-black fill with
 * no glass in it, because there was a field to blur but nothing routed a fresh one to this window.
 *
 * Desktop simplification (see this file's header comment): paints the field at this window's own
 * local bounds rather than at an absolute screen position — a `matchParentSize` box, same as the
 * Android original's mechanism minus the screen-offset math.
 */
@Composable
fun CrystalWindowContent(paintField: Boolean = true, content: @Composable () -> Unit) {
    if (!isCrystal()) {
        content()
        return
    }
    val glassBlur = koinInject<GlassBlur>()
    val field = if (isSolidField || !paintField) null else glassBlur.rememberField()
    val windowScheme = if (isSolidField) {
        MaterialTheme.colorScheme
    } else {
        MaterialTheme.colorScheme.withTranslucentSurfaces(
            crystalSurfaceAlpha(LocalCrystalStyle.current, isDark, LocalCrystalIntensity.current)
        )
    }
    val meshDark = isDark
    val meshBase = if (meshDark) AmbientBaseDark else AmbientBaseLight
    val meshBlobs = crystalFieldBlobs(LocalCrystalMesh.current, meshDark)
    MaterialTheme(colorScheme = windowScheme) {
        Box {
            if (paintField && !isSolidField) {
                Box(
                    Modifier.fillMaxSize()
                        .let { if (field != null) glassBlur.sourceModifier(field, it) else it }
                        .drawBehind { drawCrystalMeshField(meshBase, meshBlobs, meshDark) }
                )
            } else if (paintField && isSolidField) {
                Box(Modifier.fillMaxSize().background(solidFieldColor()))
            }
            CompositionLocalProvider(
                LocalGlassBlurField provides field,
                LocalCrystalInWindow provides true,
                LocalContentColor provides windowScheme.onSurface,
                content = content
            )
        }
    }
}

/**
 * The field for a `DropdownMenu`. M3 puts 8dp of padding *outside* the clipping scroll container
 * `DropdownMenu`'s content lambda draws into, but still inside the `Surface`'s own `clip(shape)` —
 * so a field painted from inside the content lambda (the [CrystalWindowContent] way) can never
 * reach that padding strip, leaving a visible gap. The field has to go on the `modifier` handed to
 * `DropdownMenu` itself instead, where `Surface` forwards it down into its content slot ahead of
 * the padding.
 *
 * Paints a fresh field at this popup's own *local* bounds — the same convention this file's header
 * comment states for every other window-scoped field ([CrystalWindowContent], [AmbientBackground]),
 * via the same [drawCrystalMeshField] call those use with no `fieldSize`/`origin` override. This
 * function used to instead sample a slice of "the enclosing window's field" via [LocalWindowInfo]'s
 * `containerSize`/`positionInWindow()`, reasoning (wrongly, on desktop) that a `DropdownMenu` popup
 * renders as an overlay within its triggering window's own composition rather than a separate
 * window. Compose Multiplatform's own docs say otherwise — "the menu is displayed in a separate
 * window, on top of other content" — so `containerSize`/`positionInWindow()` inside the popup read
 * that popup's own small, near-zero-origin window, not the main app window: the "slice" sampled was
 * wrong, and in practice rendered as a flat, near-opaque box instead of glass. Reverting to this
 * file's own local-bounds convention (already proven correct for every sheet and dialog in the app)
 * fixes that, at the cost of this function's original theoretical worry — a small popup getting a
 * miniature of the full five-blob mesh rather than a plausible "patch" of one. In practice this
 * reads fine at menu scale, same as it already does for the (similarly sized) `RhythmAlertDialog`/
 * `ModalBottomSheet` surfaces using the exact same call.
 *
 * A no-op off Crystal, and under Solid — there the shell's own flat container colour is already the
 * field, so there is nothing a gradient would add.
 */
@Composable
fun Modifier.crystalMenuField(): Modifier {
    if (!isCrystal() || isSolidField) return this
    val dark = isDark
    val base = if (dark) AmbientBaseDark else AmbientBaseLight
    val blobs = crystalFieldBlobs(LocalCrystalMesh.current, dark)
    val tint = MaterialTheme.colorScheme.surfaceContainer
    val tintAlpha = crystalSurfaceAlpha(LocalCrystalStyle.current, dark, LocalCrystalIntensity.current)
    return this.drawBehind {
        drawCrystalMeshField(base, blobs, dark)
        drawRect(tint.copy(alpha = tintAlpha))
    }
}

/**
 * The container colour for a shell whose field is supplied by [crystalMenuField]. Transparent under
 * a mesh so the field behind it shows through; off Crystal, and under Solid, it stays [fallback].
 */
@Composable
@ReadOnlyComposable
fun crystalMenuContainerColor(fallback: Color): Color =
    if (isCrystal() && !isSolidField) Color.Transparent else crystalSheetColor(fallback)
