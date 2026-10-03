package com.apagon.rhythm.ui.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.apagon.rhythm.data.preferences.ThemePreferences.Companion.MIN_CRYSTAL_MESH_CUSTOM_CHROMA
import com.apagon.rhythm.ui.components.CrystalWindowContent
import com.apagon.rhythm.ui.components.crystalControlColor
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalSliderColors
import com.apagon.rhythm.ui.components.drawCrystalMeshField
import com.apagon.rhythm.ui.theme.AmbientBaseDark
import com.apagon.rhythm.ui.theme.AmbientBaseLight
import com.apagon.rhythm.ui.theme.crystalCustomBlobs
import com.apagon.rhythm.ui.theme.habitColorPalette
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Desktop port of androidMain's `ui/util/ColorPickerComponents.kt` (Stage 11-era file, never ported
 * because nobody had added a custom-colour entry point to the desktop pickers yet — see the Settings
 * accent/Crystal-background/Crystal-mesh rows this unblocks). The dialog shell, wheel and SV square
 * are transcribed near-verbatim; the one real change is [rgbToHsv] below, since the Android original
 * leans on `android.graphics.Color.colorToHSV`, which doesn't exist on the JVM target.
 *
 * Dropped relative to Android, deliberately: the Pro paywall lock icon and the five-slot "saved
 * custom colours" row. Desktop is unconditionally Pro (Stage 6) and `ThemePreferences` only ever
 * stores one custom accent / one custom Crystal-room / one custom mesh seed — a list of saved slots
 * would be new persistence with no existing field to hang it on, for a feature nobody has asked for
 * here. A picked colour simply becomes *the* custom colour, same shape as every other single-ARGB
 * preference this screen already has.
 */

/** Pure-Kotlin replacement for `android.graphics.Color.colorToHSV`. Standard RGB->HSV conversion,
 * matching that function's convention of hue in \[0,360), saturation/value in \[0,1]. */
private fun rgbToHsv(argb: Int): Triple<Float, Float, Float> {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC

    val hue = when {
        delta == 0f -> 0f
        maxC == r -> 60f * (((g - b) / delta).mod(6f))
        maxC == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    val saturation = if (maxC == 0f) 0f else delta / maxC
    return Triple(hue, saturation, maxC)
}

/**
 * The one shell both colour-picker dialogs wear, transcribed from the Android original's
 * `ColorPickerDialogShell` (private there too — this file is the only caller either side).
 */
@Composable
private fun ColorPickerDialogShell(
    width: Dp,
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.width(width).clip(RoundedCornerShape(28.dp))) {
            CrystalWindowContent {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = crystalScaffoldColor(MaterialTheme.colorScheme.surface),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.fillMaxWidth()
                        )

                        content()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            content = actions
                        )
                    }
                }
            }
        }
    }
}

/** Generic hue-wheel + saturation/value square picker, used for the accent colour and the Crystal
 * "Solid" room colour — anywhere a single custom [Color] with no further meaning is wanted. */
@Composable
fun CustomColorPickerDialog(
    initialColor: Color = Color.Blue,
    onColorSelected: (Color) -> Unit,
    onPreview: (Color) -> Unit = {},
    onCancelPreview: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var hsv by remember {
        mutableStateOf(rgbToHsv(initialColor.toArgb()))
    }
    val cancelAndDismiss = { onCancelPreview(); onDismiss() }

    ColorPickerDialogShell(
        width = 300.dp,
        title = "Pick Custom Color",
        onDismiss = cancelAndDismiss,
        actions = {
            TextButton(onClick = cancelAndDismiss) {
                Text("Cancel")
            }
            TextButton(onClick = {
                onColorSelected(Color.hsv(hsv.first, hsv.second, hsv.third))
                onDismiss()
            }) {
                Text("Select")
            }
        }
    ) {
        ColorWheel(
            hue = hsv.first,
            onHueChanged = {
                hsv = hsv.copy(first = it)
                onPreview(Color.hsv(hsv.first, hsv.second, hsv.third))
            },
            modifier = Modifier.size(200.dp)
        )

        SaturationValueSquare(
            hue = hsv.first,
            saturation = hsv.second,
            value = hsv.third,
            onValueSelected = { s, v ->
                hsv = hsv.copy(second = s, third = v)
                onPreview(Color.hsv(hsv.first, hsv.second, hsv.third))
            },
            modifier = Modifier.size(200.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.hsv(hsv.first, hsv.second, hsv.third))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Selected Color",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "#${Integer.toHexString(Color.hsv(hsv.first, hsv.second, hsv.third).toArgb()).uppercase()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ColorWheel(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onHueChanged(calculateHue(offset, size))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onHueChanged(calculateHue(change.position, size))
                }
            }
    ) {
        val radius = size.minDimension / 2f
        val center = size.center
        val strokeWidth = 20.dp.toPx()

        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                    Color.Blue, Color.Magenta, Color.Red
                ),
                center = center
            ),
            radius = radius - strokeWidth / 2,
            style = Stroke(width = strokeWidth)
        )

        val angle = (hue / 360f) * 2 * PI
        val x = center.x + (radius - strokeWidth / 2) * cos(angle).toFloat()
        val y = center.y + (radius - strokeWidth / 2) * sin(angle).toFloat()

        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = Color.Black,
            radius = 7.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun calculateHue(offset: Offset, size: IntSize): Float {
    val center = Offset(size.width / 2f, size.height / 2f)
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    var angle = atan2(dy, dx) * 180f / PI.toFloat()
    if (angle < 0) angle += 360f
    return angle
}

@Composable
fun SaturationValueSquare(
    hue: Float,
    saturation: Float,
    value: Float,
    onValueSelected: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onValueSelected(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onValueSelected(s, v)
                }
            }
    ) {
        drawRect(color = Color.hsv(hue, 1f, 1f))
        drawRect(
            brush = Brush.horizontalGradient(colors = listOf(Color.White, Color.Transparent))
        )
        drawRect(
            brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black))
        )

        val x = saturation * size.width
        val y = (1f - value) * size.height

        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = Color.Black,
            radius = 5.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * Builds a `CrystalMesh.CUSTOM` field from one colour — the mesh-specific picker, distinct from
 * [CustomColorPickerDialog] because only the *hue* is taken (see `crystalCustomBlobs`'s own KDoc for
 * why the tones aren't the user's to pick) and because the field preview is the whole point of this
 * dialog, which a generic accent/room picker has no equivalent of.
 */
@Composable
fun CrystalCustomFieldDialog(
    initialColor: Color?,
    initialChroma: Float,
    onSave: (Color, Float) -> Unit,
    onPreview: (Color, Float) -> Unit = { _, _ -> },
    onCancelPreview: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var hsv by remember {
        mutableStateOf(rgbToHsv((initialColor ?: Color(0xFF74D0C4)).toArgb()))
    }
    var chroma by remember { mutableStateOf(initialChroma) }
    val picked = Color.hsv(hsv.first, hsv.second.coerceAtLeast(0.35f), hsv.third.coerceAtLeast(0.5f))

    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val base = if (dark) AmbientBaseDark else AmbientBaseLight
    val blobs = crystalCustomBlobs(picked, chroma, dark)
    val cancelAndDismiss = { onCancelPreview(); onDismiss() }

    ColorPickerDialogShell(
        width = 320.dp,
        title = "Custom Field",
        onDismiss = cancelAndDismiss,
        actions = {
            TextButton(onClick = cancelAndDismiss) { Text("Cancel") }
            TextButton(onClick = { onSave(picked, chroma); onDismiss() }) { Text("Save") }
        }
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .drawBehind { drawCrystalMeshField(base, blobs, dark) }
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 1f),
                            RoundedCornerShape(16.dp)
                        )
                )

                Box(contentAlignment = Alignment.Center) {
                    ColorWheel(
                        hue = hsv.first,
                        onHueChanged = {
                            hsv = hsv.copy(first = it)
                            // Recompute from the just-updated hsv rather than reusing `picked`
                            // (that val was captured at this composition's start, so it's still
                            // the previous hue until Compose recomposes).
                            onPreview(
                                Color.hsv(hsv.first, hsv.second.coerceAtLeast(0.35f), hsv.third.coerceAtLeast(0.5f)),
                                chroma
                            )
                        },
                        modifier = Modifier.size(200.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .pointerInput(Unit) { detectTapGestures { } }
                            .clip(CircleShape)
                            .background(Color.hsv(hsv.first, 1f, 1f))
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 1f),
                                CircleShape
                            )
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Depth", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = chroma,
                onValueChange = {
                    chroma = it
                    onPreview(picked, chroma)
                },
                valueRange = MIN_CRYSTAL_MESH_CUSTOM_CHROMA..1f,
                colors = crystalSliderColors()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Muted", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Vivid", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(
            text = "The disc is the hue you picked; the panel above is the field it makes. " +
                "Only the hue is used — brightness is fixed so cards and text keep the " +
                "same contrast on every field.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Desktop port of androidMain's `ColorPickerRow` (`ui/util/SharedComposables.kt`): a custom-colour
 * palette button, the app's 5 presets, and — once at least one exists — up to 5 saved custom
 * colours (long-press/right-click a saved one to remove it). `ColorPickerViewModel` and
 * `ThemePreferences.customColors` were already ported for this; only the row itself was missing.
 *
 * Dropped relative to Android: the Pro paywall lock icon and `onShowPaywall` — desktop is
 * unconditionally Pro (Stage 6), so every caller here just passes `isPro = true`. Also dropped: the
 * pastel second row (`includePastels` on Android) — desktop's `habitColorPalette` only carries the
 * 5 vivid presets, so there is nothing to show on a second row.
 */
@Composable
fun ColorPickerRow(
    colorIndex: Int,
    colorArgb: Int? = null,
    onColorSelected: (Int, Int?) -> Unit,
    viewModel: ColorPickerViewModel,
    onPreview: (Int) -> Unit = {},
    onCancelPreview: () -> Unit = {}
) {
    var showCustomPicker by remember { mutableStateOf(false) }
    val customColors by viewModel.customColors.collectAsState()

    val isCustomSelected = colorArgb != null && habitColorPalette.none { it.toArgb() == colorArgb }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f).aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (isCustomSelected) Color(colorArgb!!)
                            else crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest)
                        )
                        .border(
                            2.dp,
                            if (isCustomSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .clickable { showCustomPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Custom Color",
                        tint = if (isCustomSelected) {
                            if (Color(colorArgb!!).luminance() > 0.5f) Color.Black else Color.White
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            habitColorPalette.forEachIndexed { index, color ->
                val isSelected = colorIndex == index && colorArgb == null
                Box(
                    modifier = Modifier
                        .weight(1f).aspectRatio(1f)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onColorSelected(index, null) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        val savedSlots = customColors.withIndex().filter { it.value != null }
        if (savedSlots.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.weight(1f))
                savedSlots.forEach { (index, argb) ->
                    val colour = argb!!
                    val isSelected = colorArgb == colour
                    Box(
                        modifier = Modifier
                            .weight(1f).aspectRatio(1f)
                            .clip(CircleShape)
                            .background(Color(colour))
                            .combinedClickable(
                                onClick = { onColorSelected(-1, colour) },
                                onLongClick = { viewModel.removeCustomColor(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (Color(colour).luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                repeat(customColors.size - savedSlots.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialColor = if (colorArgb != null)
                Color(colorArgb)
            else
                habitColorPalette[colorIndex.coerceIn(0, habitColorPalette.lastIndex)],
            onColorSelected = { newColor ->
                val newArgb = newColor.toArgb()
                if (customColors.none { it == newArgb }) {
                    customColors.indexOfFirst { it == null }
                        .takeIf { it != -1 }
                        ?.let { viewModel.saveCustomColor(it, newArgb) }
                }
                onColorSelected(-1, newArgb)
            },
            onPreview = { previewColor -> onPreview(previewColor.toArgb()) },
            onCancelPreview = onCancelPreview,
            onDismiss = { showCustomPicker = false }
        )
    }
}
