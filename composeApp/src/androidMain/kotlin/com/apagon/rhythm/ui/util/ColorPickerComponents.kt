package com.apagon.rhythm.ui.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.*

@Composable
fun CustomColorPickerDialog(
    initialColor: Color = Color.Blue,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hsv by remember {
        val hsvArray = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsvArray)
        mutableStateOf(Triple(hsvArray[0], hsvArray[1], hsvArray[2]))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Pick Custom Color",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth()
                )

                ColorWheel(
                    hue = hsv.first,
                    onHueChanged = { hsv = hsv.copy(first = it) },
                    modifier = Modifier.size(200.dp)
                )

                SaturationValueSquare(
                    hue = hsv.first,
                    saturation = hsv.second,
                    value = hsv.third,
                    onValueSelected = { s, v -> hsv = hsv.copy(second = s, third = v) },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(onClick = {
                        onColorSelected(Color.hsv(hsv.first, hsv.second, hsv.third))
                        onDismiss()
                    }) {
                        Text("Select")
                    }
                }
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

        // Hue indicator
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
        // Hue background
        drawRect(color = Color.hsv(hue, 1f, 1f))

        // White to transparent (saturation)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White, Color.Transparent)
            )
        )

        // Black to transparent (value)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black)
            )
        )

        // Selection indicator
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
