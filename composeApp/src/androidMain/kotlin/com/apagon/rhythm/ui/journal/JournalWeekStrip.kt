package com.apagon.rhythm.ui.journal
import com.apagon.rhythm.core.time.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.DateTimeFormatter
import com.apagon.rhythm.core.time.TextStyle
import com.apagon.rhythm.core.time.ChronoUnit
import java.util.Locale

@Composable
fun JournalWeekStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val initialPage = 500
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 1000 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedDate) {
        val startOfSelectedWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val startOfTodayWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekOffset = ChronoUnit.WEEKS.between(startOfTodayWeek, startOfSelectedWeek).toInt()
        val targetPage = initialPage + weekOffset
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    var displayedMonth by remember(selectedDate) { mutableStateOf(selectedDate.withDayOfMonth(1)) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "chevron_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(expanded) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 20f && !expanded) onToggleExpanded()
                    else if (dragAmount < -20f && expanded) onToggleExpanded()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = !expanded,
                enter = fadeIn(tween(200)) + expandHorizontally(),
                exit = fadeOut(tween(150)) + shrinkHorizontally(),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Week",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        userScrollEnabled = true
                    ) { page ->
                        val weekOffset = page - initialPage
                        val startOfPagerWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).plusWeeks(weekOffset.toLong())

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (0..6).forEach { offset ->
                                val date = startOfPagerWeek.plusDays(offset.toLong())
                                DayChip(
                                    date = date,
                                    isSelected = date == selectedDate,
                                    isToday = date == today,
                                    onClick = { onDateSelected(date) }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Week",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (expanded) { Spacer(Modifier.weight(1f)) }

            IconButton(
                onClick = onToggleExpanded,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse calendar" else "Expand calendar",
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                expandFrom = Alignment.Top
            ) + fadeIn(tween(200)),
            exit = shrinkVertically(
                animationSpec = tween(250, easing = FastOutLinearInEasing),
                shrinkTowards = Alignment.Top
            ) + fadeOut(tween(200))
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            onDateSelected(today)
                            displayedMonth = today.withDayOfMonth(1)
                        }) {
                            Icon(Icons.Default.Today, contentDescription = "Today", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }

                    val firstDay = displayedMonth.withDayOfMonth(1)
                    // Sunday = 7 → 0, Monday = 1 → 1, …, Saturday = 6 → 6
                    val startOffset = firstDay.dayOfWeek.value % 7
                    val daysInMonth = displayedMonth.lengthOfMonth()
                    val weeks = (startOffset + daysInMonth + 6) / 7

                    repeat(weeks) { week ->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { dow ->
                                val dayIdx = week * 7 + dow - startOffset + 1
                                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                    if (dayIdx in 1..daysInMonth) {
                                        val date = displayedMonth.withDayOfMonth(dayIdx)
                                        DayChip(
                                            date = date,
                                            isSelected = date == selectedDate,
                                            isToday = date == today,
                                            showDayLabel = false,
                                            onClick = {
                                                onDateSelected(date)
                                                onToggleExpanded()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayChip(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    showDayLabel: Boolean = true,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val labelColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday    -> MaterialTheme.colorScheme.primary
        else       -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderModifier = if (isToday && !isSelected) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
    } else Modifier

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(borderModifier)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = if (showDayLabel) 6.dp else 8.dp)
            .width(36.dp)
    ) {
        if (showDayLabel) {
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(
            text = date.dayOfMonth.toString(),
            style = if (showDayLabel) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
            color = labelColor,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}
