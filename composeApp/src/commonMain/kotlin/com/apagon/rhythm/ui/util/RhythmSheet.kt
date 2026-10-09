package com.apagon.rhythm.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.components.CrystalWindowContent
import com.apagon.rhythm.ui.components.isCrystal
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.focusable

/**
 * Desktop adaptation of the Android original's `RhythmSheet` (`ui/util/RhythmSheet.kt`) — the one
 * bottom sheet shell every add/edit sheet in this app should go through, instead of hand-rolling
 * `ModalBottomSheet` directly.
 *
 * **Why this exists**: every sheet in this app used to pass `containerColor =
 * crystalSheetColor(fallback = ...)` straight into `ModalBottomSheet`. Under Crystal that function
 * returns a single flat gradient-stop color (see `Crystal.kt`'s `crystalSheetColor`) — exactly the
 * bug the Android original's own doc comment describes fixing: "one flat colour standing in for a
 * diagonal gradient, which is what put a mint band across the top of every sheet." The fix there,
 * ported here unchanged in spirit: make the sheet's own `containerColor` transparent under Crystal,
 * disable Material's built-in drag handle (`dragHandle = null`), and draw a replacement handle
 * strip *inside* [CrystalWindowContent]'s glass field instead — so the handle sits on the same
 * gradient as the rest of the sheet rather than on a separately-colored strip above it.
 *
 * Ported shape, not ported pixel-for-pixel: the Android original also reserves status-bar/cutout
 * insets and installs a nested-scroll "dead zone" so the sheet's own swipe-to-dismiss can't be
 * stolen by inner scrollables — both exist only because a phone screen has a notch and a hard edge.
 * Neither applies to a resizable desktop window, so both are omitted here. [maxWidth] stands in for
 * the Android original's `displayMetrics`-based cap, matching the pattern already used by
 * [RhythmAlertDialog] and [RhythmDatePickerDialog] on this platform.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RhythmSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = if (isCrystal()) Color.Transparent else BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor).takeOrElse { MaterialTheme.colorScheme.onSurface },
    fillHeight: Boolean = false,
    maxWidth: Dp = 560.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = if (fillHeight) modifier.fillMaxHeight() else modifier,
        sheetState = sheetState,
        containerColor = containerColor,
        contentColor = contentColor,
        dragHandle = null
    ) {
        // Escape closes the sheet, the desktop's equivalent of the phone's back gesture. Preview
        // (parent-first) so it also works while a text field inside has focus; the sheet itself
        // takes focus on open so Escape works before anything is clicked.
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
        CrystalWindowContent {
            Column(
                modifier = (if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { e ->
                        if (e.key == Key.Escape && e.type == KeyEventType.KeyDown) { onDismiss(); true } else false
                    }
                    .widthIn(max = maxWidth)
                    .align(Alignment.CenterHorizontally)
            ) {
                SheetDismissHandle()
                Column(
                    modifier = if (fillHeight) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                    content = content
                )
            }
        }
    }
}

/** Full-width grab strip with the standard pill, drawn on the sheet's own glass field rather than
 * in Material's separately-colored handle slot — see [RhythmSheet]'s doc comment for why. */
@Composable
private fun SheetDismissHandle() {
    Box(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
    }
}
