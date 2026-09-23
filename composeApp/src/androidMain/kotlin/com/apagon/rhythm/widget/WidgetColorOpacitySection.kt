package com.apagon.rhythm.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.util.CustomColorPickerDialog
import kotlin.math.roundToInt

private val STYLE_OPTIONS = listOf(
    WidgetColors.STYLE_DEFAULT to "Default",
    WidgetColors.STYLE_APP_THEME to "App Theme",
    WidgetColors.STYLE_CUSTOM to "Custom",
    WidgetColors.STYLE_SYSTEM to "System",
)

@Composable
fun WidgetColorOpacitySection(
    style: String,
    customColorArgb: Int?,
    opacity: Float,
    fallbackColorArgb: Int,
    onStyleChange: (String) -> Unit,
    onCustomColorChange: (Int?) -> Unit,
    onOpacityChange: (Float) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val systemAvailable = WidgetColors.systemStyleAvailable()
    val visibleOptions = if (systemAvailable) STYLE_OPTIONS else STYLE_OPTIONS.dropLast(1)
    val resolvedArgb = customColorArgb ?: fallbackColorArgb
    val palette = WidgetColors.resolvePalette(context, style, customColorArgb, fallbackColorArgb, opacity)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Widget Style",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            visibleOptions.forEachIndexed { index, (key, label) ->
                SegmentedButton(
                    selected = style == key,
                    onClick = { onStyleChange(key) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = visibleOptions.size)
                ) {
                    Text(label)
                }
            }
        }

        if (style == WidgetColors.STYLE_SYSTEM) {
            Text(
                text = "Matches your wallpaper's Material You colors, like Google's own widgets. Requires Android 12+.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (style == WidgetColors.STYLE_CUSTOM) {
            Text(
                text = "Widget Color",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WidgetColors.PRESET_ARGBS.forEach { presetArgb ->
                    ColorSwatch(
                        color = Color(presetArgb),
                        isSelected = customColorArgb == presetArgb,
                        onClick = { onCustomColorChange(presetArgb) }
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { showPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Colorize,
                        contentDescription = "Custom color",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (style != WidgetColors.STYLE_DEFAULT && style != WidgetColors.STYLE_SYSTEM) {
            Text(
                text = "Opacity: ${(opacity * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = opacity,
                onValueChange = onOpacityChange,
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(palette.cardBackground)
                .border(1.dp, Color(resolvedArgb).copy(alpha = 0.22f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showPicker) {
        CustomColorPickerDialog(
            initialColor = Color(resolvedArgb),
            onColorSelected = { onCustomColorChange(it.toArgbInt()) },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = contrastingOnColor(color),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun contrastingOnColor(color: Color): Color =
    if (color.luminance() > 0.5f) Color.Black else Color.White

private fun Color.luminance(): Float = (0.299f * red + 0.587f * green + 0.114f * blue)

private fun Color.toArgbInt(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt(),
    (red * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue * 255).roundToInt()
)
