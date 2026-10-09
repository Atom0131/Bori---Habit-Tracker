package com.apagon.rhythm.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalFabContainerColor
import com.apagon.rhythm.ui.components.crystalFabContentColor
import com.apagon.rhythm.ui.components.crystalFabElevation
import com.apagon.rhythm.ui.components.crystalFabSurface
import com.apagon.rhythm.ui.components.crystalStickyHeader
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.IconButton

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
 * Shared "add" FAB, consolidating the identical `FloatingActionButton` block that Journal, Notes,
 * NotebookDetail, and Calendar each hand-rolled separately (same crystalFab* styling, just a
 * different onClick). Pass `modifier` for placement — `.align(Alignment.BottomEnd).padding(...)`
 * when overlaid in a `Box`, or nothing when passed straight into `Scaffold(floatingActionButton =
 * {})`, which positions it itself.
 *
 * Draws the plus from two bars rather than a `Text("+")` glyph or `Icons.Default.Add` (this
 * project has no material-icons-core dependency available — confirmed by a direct compile
 * attempt, not just material-icons-extended, which Stage 18b already found broke dependency
 * resolution): a font glyph's ascent/descent box isn't symmetric around the "+" shape itself, so
 * centering the `Text` composable in the FAB still left the visible plus sign off-center. Two
 * bars overlaid in a centered [Box] are geometrically centered regardless of font metrics.
 */
@Composable
fun RhythmAddFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.crystalFabSurface(),
        containerColor = crystalFabContainerColor(),
        contentColor = crystalFabContentColor(),
        elevation = crystalFabElevation(),
        shape = CircleShape
    ) {
        val plusColor = LocalContentColor.current
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(2.dp).height(16.dp).background(plusColor, RoundedCornerShape(1.dp)))
            Box(Modifier.width(16.dp).height(2.dp).background(plusColor, RoundedCornerShape(1.dp)))
        }
    }
}

/**
 * Port of the Android app's `CollapsibleSectionHeader`, with the same `ExpandLess`/`ExpandMore`
 * chevrons (it used ▴/▾ text glyphs until 2026-10-09, from when desktop had no icon dependency).
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
            .padding(vertical = 4.dp)
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
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The round check the Android app uses for habits and to-dos (`HabitRowComposable`,
 * `TodoRowComposable`): an empty circle, or a filled check circle in the accent colour. The square
 * Material `Checkbox` the desktop used read as a different app under Crystal. Note checklists and
 * settings options keep a square checkbox on both platforms, on purpose.
 */
@Composable
fun RoundCheck(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (checked) "Completed" else "Not completed",
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Port of the Android app's `SectionEmptyCard`: an empty planner section shows a glass card, not
 * loose text. Under a glass heading, bare text read as nothing, so an empty section looked like
 * its toggle did nothing.
 */
@Composable
fun SectionEmptyCard(text: String, horizontalPadding: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 4.dp)
            .crystalCardSurface()
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        )
    }
}
