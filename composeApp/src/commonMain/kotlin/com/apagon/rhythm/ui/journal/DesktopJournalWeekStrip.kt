package com.apagon.rhythm.ui.journal

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
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalSelectedChipColor
import com.apagon.rhythm.ui.components.crystalSelectedChipContentColor
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import com.apagon.rhythm.ui.util.CrystalDayChip

/**
 * Desktop port of JournalWeekStrip.kt. Structurally the same (a pager week
 * row that expands into a month grid) but WITHOUT androidx.compose.animation
 * (AnimatedVisibility/animateFloatAsState/tween/spring) — that artifact isn't
 * declared as a commonMain dependency (only compose.foundation/material3/ui
 * are), and adding a new library dependency as a side effect of one stage
 * would be exactly the unplanned scope expansion Stage 8 already ruled out
 * for material-icons-extended. Expand/collapse here is a plain conditional
 * swap instead of an animated cross-fade. HorizontalPager itself IS part of
 * compose.foundation (verified directly against the resolved jar) and is
 * kept, since it costs no new dependency.
 */
@Composable
fun DesktopJournalWeekStrip(
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!expanded) {
                TextButton(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous") }

                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    val weekOffset = page - initialPage
                    val startOfPagerWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).plusWeeks(weekOffset.toLong())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        (0..6).forEach { offset ->
                            val date = startOfPagerWeek.plusDays(offset.toLong())
                            DesktopDayChip(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == today,
                                onClick = { onDateSelected(date) }
                            )
                        }
                    }
                }

                TextButton(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next") }
            } else {
                Spacer(Modifier.weight(1f))
            }

            TextButton(onClick = onToggleExpanded) {
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = if (expanded) "Collapse" else "Expand")
            }
        }

        if (expanded) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                    .crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous") }
                        Text(
                            text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next") }
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
                                        DesktopDayChip(
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
fun DesktopDayChip(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    showDayLabel: Boolean = true,
    onClick: () -> Unit
) {
    CrystalDayChip(date, isSelected, isToday, hasIndicator = false, showDayLabel = showDayLabel, onClick = onClick)
}
