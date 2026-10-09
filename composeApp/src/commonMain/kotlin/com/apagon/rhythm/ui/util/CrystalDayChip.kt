package com.apagon.rhythm.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.components.crystalChipSurface
import com.apagon.rhythm.ui.components.isCrystal
import com.apagon.rhythm.core.time.TextStyle
import com.apagon.rhythm.core.time.getDisplayName
import kotlinx.datetime.LocalDate

/**
 * Port of the Android app's `DayChip` (`WeekStripComposable.kt`), shared by the Today and Journal
 * week strips. Under Crystal the selected day is a glass pill with accent text, not a solid disc,
 * and today keeps only accent text so it never reads louder than the selected day. The dot is drawn
 * only on days that have something; other days reserve its height so the row doesn't jump.
 */
@Composable
fun CrystalDayChip(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasIndicator: Boolean,
    showDayLabel: Boolean = true,
    onClick: () -> Unit
) {
    val crystal = isCrystal()
    val bgColor = when {
        isSelected -> if (crystal) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primary
        isToday -> if (crystal) Color.Transparent else MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val labelColor = when {
        isSelected -> if (crystal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
        isToday -> if (crystal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    // Only the selected chip is real glass; glass on every day would put a pill on all seven.
    val chipSurface = if (isSelected && crystal) Modifier.crystalChipSurface(fill = bgColor)
                      else Modifier.clip(CircleShape).background(bgColor)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(chipSurface)
            .clip(CircleShape)
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
        if (hasIndicator) {
            Box(
                Modifier.padding(top = 2.dp).size(4.dp).clip(CircleShape)
                    .background(if (isSelected && !crystal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
            )
        } else {
            Spacer(Modifier.height(6.dp))
        }
    }
}
