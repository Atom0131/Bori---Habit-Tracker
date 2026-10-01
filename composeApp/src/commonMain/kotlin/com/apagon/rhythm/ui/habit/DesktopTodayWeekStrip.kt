package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalSelectedChipColor
import com.apagon.rhythm.ui.components.crystalSelectedChipContentColor
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * Port of Android's `WeekStrip` (`ui/habit/WeekStripComposable.kt`) for desktop's Today screen —
 * always-visible between `HomeHeader` and the habit-frequency sections, matching Android's list
 * layout (Stage 19f). Structurally a copy of [com.apagon.rhythm.ui.journal.DesktopJournalWeekStrip]
 * (same pager-week/expand-to-month-grid shape, same no-compose.animation constraint — see that
 * file's doc comment), kept as its own copy rather than a shared one because this version adds the
 * `indicatorDates` dot Android's Today `WeekStrip` has for habit/event days, which Journal's never
 * needed.
 */
@Composable
fun DesktopTodayWeekStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    indicatorDates: Set<String> = emptySet()
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!expanded) {
                TextButton(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) { Text("‹") }

                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    val weekOffset = page - initialPage
                    val startOfPagerWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).plusWeeks(weekOffset.toLong())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        (0..6).forEach { offset ->
                            val date = startOfPagerWeek.plusDays(offset.toLong())
                            DesktopTodayDayChip(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == today,
                                hasIndicator = date.format(ISO_LOCAL_DATE) in indicatorDates,
                                onClick = { onDateSelected(date) }
                            )
                        }
                    }
                }

                TextButton(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) { Text("›") }
            } else {
                Spacer(Modifier.weight(1f))
            }

            TextButton(onClick = onToggleExpanded) {
                Text(if (expanded) "▴" else "▾")
            }
        }

        if (expanded) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                    .crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) { Text("‹") }
                        Text(
                            text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) { Text("›") }
                        TextButton(onClick = {
                            onDateSelected(today)
                            displayedMonth = today.withDayOfMonth(1)
                        }) { Text("Today") }
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
                                        DesktopTodayDayChip(
                                            date = date,
                                            isSelected = date == selectedDate,
                                            isToday = date == today,
                                            hasIndicator = date.format(ISO_LOCAL_DATE) in indicatorDates,
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
private fun DesktopTodayDayChip(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasIndicator: Boolean,
    showDayLabel: Boolean = true,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) crystalSelectedChipColor(MaterialTheme.colorScheme.primary) else Color.Transparent
    val labelColor = when {
        isSelected -> crystalSelectedChipContentColor(MaterialTheme.colorScheme.onPrimary)
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                text = date.dayOfWeek.getDisplayName(TextStyle.NARROW),
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
        Box(
            modifier = Modifier.size(4.dp).clip(CircleShape)
                .background(if (hasIndicator) labelColor.copy(alpha = 0.8f) else Color.Transparent)
        )
    }
}
