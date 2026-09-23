package com.apagon.rhythm.ui.util

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun MaterialDurationPicker(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onChanged: (h: Int, m: Int, s: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp,
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                label = "h",
                value = hours,
                range = 0..99,
                onValueChange = { onChanged(it, minutes, seconds) }
            )
            Separator()
            WheelPicker(
                label = "m",
                value = minutes,
                range = 0..59,
                onValueChange = { onChanged(hours, it, seconds) }
            )
            Separator()
            WheelPicker(
                label = "s",
                value = seconds,
                range = 0..59,
                onValueChange = { onChanged(hours, minutes, it) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = 60.dp

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentValue by rememberUpdatedState(value)

    // Only report a settled index AFTER the user actually started scrolling.
    // Using isScrollInProgress as a LaunchedEffect key fires on composition entry
    // (including when returning from edit mode) before animateScrollToItem has run,
    // which would overwrite a freshly typed value with the old wheel position.
    LaunchedEffect(listState) {
        var userScrolled = false
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) {
                    userScrolled = true
                } else if (userScrolled) {
                    userScrolled = false
                    val snappedIndex = listState.firstVisibleItemIndex
                    if (snappedIndex != currentValue) {
                        currentOnValueChange(snappedIndex)
                    }
                }
            }
    }

    // Sync from external value changes (e.g. after typing a value in edit mode).
    LaunchedEffect(value) {
        if (listState.firstVisibleItemIndex != value) {
            listState.animateScrollToItem(value)
        }
    }

    // Direct Input State
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(itemHeight * 3), // Show 3 items: previous, selected, next
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isEditing,
            transitionSpec = { fadeIn(tween(200, easing = EaseInOut)) togetherWith fadeOut(tween(150, easing = EaseInOut)) },
            label = "durationPickerMode"
        ) { editing ->
        if (editing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isEditing = false },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = editValue,
                        onValueChange = {
                            if (it.length <= 2 && it.all { c -> c.isDigit() }) {
                                editValue = it
                                if (it.length == 2) {
                                    val newVal = it.toInt().coerceIn(range)
                                    onValueChange(newVal)
                                    isEditing = false
                                }
                            }
                        },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .width(60.dp),
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (editValue.isNotEmpty()) {
                                    val newVal = editValue.toInt().coerceIn(range)
                                    onValueChange(newVal)
                                }
                                isEditing = false
                            }
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        } else {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = itemHeight), // Provides space for top/bottom unselected items
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(range.last - range.first + 1) { index ->
                    val num = range.first + index
                    val isSelected = listState.firstVisibleItemIndex == index

                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) {
                                    editValue = ""
                                    isEditing = true
                                } else {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(index)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = num.toString().padStart(2, '0'),
                                style = if (isSelected) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.alpha(if (isSelected) 1f else 0.5f)
                            )
                            if (isSelected) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        } // end AnimatedContent

        // Selection highlight — direct child of outer Box so contentAlignment = Center
        // places it at y=60..120dp (middle row) over the selected item.
        if (!isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
private fun Separator() {
    Text(
        text = ":",
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
        fontWeight = FontWeight.Light
    )
}
