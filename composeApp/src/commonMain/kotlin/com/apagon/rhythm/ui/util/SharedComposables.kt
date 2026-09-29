package com.apagon.rhythm.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.components.crystalStickyHeader

/**
 * Port of the Android app's `PlannerMetrics` (`ui/util/SharedComposables.kt`) — a heading is wider
 * than the rows beneath it, which is what makes it visibly contain them.
 */
object PlannerMetrics {
    /** Headings — the outer edge, because a heading visually contains its rows. */
    val SectionInset = 8.dp

    /** Rows — indented inside their heading. */
    val RowInset = 16.dp
}

/** The two weights a planner heading comes in — [Primary] names a list, [Secondary] qualifies one. */
enum class SectionHeaderTier { Primary, Secondary }

/**
 * Port of the Android app's `CollapsibleSectionHeader`. Desktop has no `material-icons-extended`
 * dependency (see `DesktopJournalWeekStrip.kt`'s expand/collapse chevron), so the expand/collapse
 * affordance is a plain glyph via `Text`, matching that existing convention, rather than an
 * `Icons.Default.ExpandMore`/`ExpandLess` pair.
 */
@Composable
fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    tier: SectionHeaderTier = SectionHeaderTier.Secondary,
    horizontalPadding: Dp = 16.dp
) {
    val primary = tier == SectionHeaderTier.Primary
    Box(
        Modifier
            .fillMaxWidth()
            .crystalStickyHeader(horizontalPadding = PlannerMetrics.SectionInset)
            .clickable(onClick = onToggle)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = if (primary) 16.dp else 12.dp
            )
        ) {
            Text(
                text = title,
                style = if (primary) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelMedium,
                color = if (primary) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                fontWeight = if (primary) FontWeight.Medium else FontWeight.Bold
            )
            Text(
                text = if (expanded) "▴" else "▾",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
