package com.apagon.rhythm.ui.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.apagon.rhythm.ui.components.CrystalWindowContent
import com.apagon.rhythm.ui.components.crystalScaffoldColor

/**
 * Desktop adaptation of the Android original's `RhythmAlertDialog` (`ui/util/RhythmSheet.kt`) —
 * the shell every dialog in that app goes through instead of stock M3's `AlertDialog`, so a dialog
 * gets its own Crystal field/blur source ([CrystalWindowContent]) instead of rendering as a flat
 * opaque box. Ported shape, not ported pixel-for-pixel: the Android original clips its own width to
 * `screenWidth - margin`, measured off `displayMetrics`, because a phone screen is the actual
 * constraint a dialog has to fit; this app's window is user-resizable with no fixed "screen" to
 * measure against, so a flat [maxWidth] cap stands in for that math.
 *
 * Covers every stock `AlertDialog(...)` call site in this port, including the ones that wrap a bare
 * `TimePicker` as their `text` slot — this app never built Android's custom dial-picker UI, so a
 * themed generic alert shell is the only shell a "time picker" needs here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RhythmAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = Color.Unspecified,
    maxWidth: Dp = 560.dp,
    properties: DialogProperties = DialogProperties()
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier, properties = properties) {
        Box(Modifier.widthIn(max = maxWidth).clip(shape)) {
            CrystalWindowContent {
                Surface(
                    shape = shape,
                    color = crystalScaffoldColor(
                        if (containerColor.isSpecified) containerColor else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        icon?.invoke()
                        title?.let { ProvideTextStyle(MaterialTheme.typography.headlineSmall, it) }
                        text?.let { ProvideTextStyle(MaterialTheme.typography.bodyMedium, it) }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}
