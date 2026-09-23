package com.apagon.rhythm.ui.util
import com.apagon.rhythm.core.time.*

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlinx.datetime.LocalTime

private val ITEM_HEIGHT = 48.dp
private const val VISIBLE_ITEMS = 5

/**
 * A single scrollable drum-roll column.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    if (items.isEmpty()) return

    val paddedItems = remember(items) { listOf("", "") + items + listOf("", "") }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex)
    )

    // Capture vertical drag to prevent it from bubbling up to the BottomSheet
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (source == NestedScrollSource.UserInput) {
                    // Consume Y to stop parent (BottomSheet) from dragging
                    Offset(0f, available.y)
                } else Offset.Zero
            }
        }
    }

    // Report index when scroll settles
    LaunchedEffect(listState) {
        var lastReported = selectedIndex
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .filter { (scrolling, _) -> !scrolling }
            .collect { (_, idx) ->
                val newIndex = idx.coerceIn(0, items.lastIndex)
                if (newIndex != lastReported) {
                    lastReported = newIndex
                    onIndexChanged(newIndex)
                }
            }
    }

    // Sync programmatic changes
    LaunchedEffect(selectedIndex) {
        val clamped = selectedIndex.coerceIn(0, items.lastIndex)
        if (listState.firstVisibleItemIndex != clamped && !listState.isScrollInProgress) {
            listState.animateScrollToItem(clamped)
        }
    }

    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .height(ITEM_HEIGHT * VISIBLE_ITEMS)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Selection highlight band
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT)
                    .background(highlightColor, shape = RoundedCornerShape(8.dp))
            )

            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                items(paddedItems.size) { i ->
                    val isSelected = (i - 2) == selectedIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ITEM_HEIGHT),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = paddedItems[i],
                            style = if (isSelected)
                                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                            else
                                MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = onSurface.copy(alpha = if (isSelected) 1f else 0.38f)
                        )
                    }
                }
            }

            // Gradients
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT * 2)
                    .background(Brush.verticalGradient(listOf(surfaceColor, Color.Transparent)))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT * 2)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, surfaceColor)))
            )
        }

        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun WheelTimePicker(
    time: LocalTime,
    onTimeChanged: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    
    val hourItems = remember(is24Hour) {
        if (is24Hour) (0..23).map { it.toString().padStart(2, '0') }
        else (1..12).map { it.toString() }
    }
    val minuteItems = remember { (0..59).map { it.toString().padStart(2, '0') } }
    val amPmItems = listOf("AM", "PM")

    val hourIndex = if (is24Hour) time.hour else {
        val h = time.hour % 12
        if (h == 0) 11 else h - 1
    }
    val minuteIndex = time.minute
    val amPmIndex = if (time.hour < 12) 0 else 1

    fun updateTime(hIdx: Int = hourIndex, mIdx: Int = minuteIndex, isAm: Boolean = (amPmIndex == 0)) {
        val newHour = if (is24Hour) hIdx else {
            val h12 = hIdx + 1
            if (isAm) (if (h12 == 12) 0 else h12)
            else (if (h12 == 12) 12 else h12 + 12)
        }
        onTimeChanged(LocalTime.of(newHour, mIdx))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPickerColumn(
                items = hourItems,
                selectedIndex = hourIndex,
                onIndexChanged = { updateTime(hIdx = it) },
                modifier = Modifier.width(64.dp)
            )
            Text(":", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 8.dp))
            WheelPickerColumn(
                items = minuteItems,
                selectedIndex = minuteIndex,
                onIndexChanged = { updateTime(mIdx = it) },
                modifier = Modifier.width(64.dp)
            )
            if (!is24Hour) {
                Spacer(Modifier.width(16.dp))
                WheelPickerColumn(
                    items = amPmItems,
                    selectedIndex = amPmIndex,
                    onIndexChanged = { updateTime(isAm = (it == 0)) },
                    modifier = Modifier.width(64.dp)
                )
            }
        }
    }
}

@Composable
fun WheelDurationPicker(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onChanged: (h: Int, m: Int, s: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hourItems = remember { (0..99).map { it.toString().padStart(2, '0') } }
    val minuteItems = remember { (0..59).map { it.toString().padStart(2, '0') } }
    val secondItems = remember { (0..59).map { it.toString().padStart(2, '0') } }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPickerColumn(
                items = hourItems,
                selectedIndex = hours.coerceIn(0, 99),
                onIndexChanged = { onChanged(it, minutes, seconds) },
                modifier = Modifier.width(64.dp),
                label = "h"
            )
            Text(":", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 20.dp))
            WheelPickerColumn(
                items = minuteItems,
                selectedIndex = minutes.coerceIn(0, 59),
                onIndexChanged = { onChanged(hours, it, seconds) },
                modifier = Modifier.width(64.dp),
                label = "m"
            )
            Text(":", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 20.dp))
            WheelPickerColumn(
                items = secondItems,
                selectedIndex = seconds.coerceIn(0, 59),
                onIndexChanged = { onChanged(hours, minutes, it) },
                modifier = Modifier.width(64.dp),
                label = "s"
            )
        }
    }
}
