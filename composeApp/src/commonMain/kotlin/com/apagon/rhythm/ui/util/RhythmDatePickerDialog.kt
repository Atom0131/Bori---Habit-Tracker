package com.apagon.rhythm.ui.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apagon.rhythm.ui.components.CrystalWindowContent
import com.apagon.rhythm.ui.components.crystalScaffoldColor

/**
 * Desktop adaptation of the Android original's `RhythmDatePickerDialog` (`ui/util/
 * RhythmDatePickerDialog.kt`) — replaces stock M3 `DatePickerDialog`, which paints its own opaque
 * container from outside the content slot a [CrystalWindowContent] wrap could ever reach (the same
 * reason `RhythmAlertDialog` exists). A raw [Dialog] rather than `BasicAlertDialog`, matching the
 * Android shell — `DatePickerDialog` itself is a raw `Dialog` under the hood, not an alert.
 *
 * The caller's own `DatePicker(...)` call (passed as [content]) must set
 * `colors = DatePickerDefaults.colors(containerColor = Color.Transparent)` so the picker doesn't
 * paint an opaque rectangle over the field this shell provides — matching the Android original's
 * own "suppress M3's own title, let the shell draw its header outside M3's chrome" approach, this
 * shell draws no title of its own either; pass one via [content] if needed.
 */
@Composable
fun RhythmDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    maxWidth: Dp = 560.dp,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier.widthIn(max = maxWidth).clip(shape)) {
            CrystalWindowContent {
                Surface(
                    shape = shape,
                    color = crystalScaffoldColor(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 6.dp
                ) {
                    Column {
                        content()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}
