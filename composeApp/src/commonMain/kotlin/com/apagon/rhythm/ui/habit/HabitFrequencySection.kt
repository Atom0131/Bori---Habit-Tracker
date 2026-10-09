package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.util.CollapsibleSectionHeader
import com.apagon.rhythm.ui.util.RoundCheck
import com.apagon.rhythm.ui.util.PlannerMetrics
import com.apagon.rhythm.ui.util.SectionHeaderTier
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.foundation.onClick
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.ExperimentalFoundationApi
import com.apagon.rhythm.ui.util.SectionEmptyCard
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box

/**
 * Desktop port of the Android app's `habitFrequencySection` (`ui/habit/HabitFrequencySection.kt`):
 * a section heading, its outstanding rows, and its finished ones folded behind a quiet "N done"
 * line. Today calls it once, as one combined "Habits" section across every frequency, like Android
 * since 2026-10-04 (separate Daily/Weekly/Monthly sections were mostly permanently-empty clutter).
 * An empty section shows [SectionEmptyCard], as every other planner section does.
 *
 * Still narrower than Android: no checklist items and no swipe-to-archive/delete.
 */
internal fun LazyListScope.habitFrequencySection(
    sectionKey: String,
    title: String,
    pending: List<Habit>,
    completed: List<Habit>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    doneExpanded: Boolean,
    onToggleDone: () -> Unit,
    onToggleCompletion: (habitId: Long, isDone: Boolean) -> Unit,
    onView: (Habit) -> Unit,
    onEdit: (Habit) -> Unit,
    isDueToday: Boolean = false,
    emptyLabel: String = "No habits yet"
) {
    item(key = "${sectionKey}_header") {
        CollapsibleSectionHeader(
            title = title,
            expanded = expanded,
            onToggle = onToggleExpanded,
            tier = SectionHeaderTier.Primary
        )
    }

    if (!expanded) return

    if (pending.isEmpty() && completed.isEmpty()) {
        item(key = "${sectionKey}_empty") { SectionEmptyCard(emptyLabel, horizontalPadding = DesktopLayout.screenPadding) }
        return
    }

    items(pending, key = { it.id }) { habit ->
        DesktopHabitRow(
            habit = habit,
            isDone = false,
            onToggle = { onToggleCompletion(habit.id, false) },
            onClick = { onView(habit) },
            onEdit = { onEdit(habit) },
            isDueToday = isDueToday
        )
    }

    if (completed.isEmpty()) return

    item(key = "${sectionKey}_done_toggle") {
        SectionFoldToggle(
            label = "${completed.size} done",
            expanded = doneExpanded,
            onToggle = onToggleDone
        )
    }

    if (doneExpanded) {
        items(completed, key = { "done_${it.id}" }) { habit ->
            DesktopHabitRow(
                habit = habit,
                isDone = true,
                onToggle = { onToggleCompletion(habit.id, true) },
                onClick = { onView(habit) },
                onEdit = { onEdit(habit) }
            )
        }
    }
}

/**
 * Port of Android's `HabitRow` (`HabitRowComposable.kt`), minus swipe actions: round check, name
 * (struck through once done), a small pencil to edit, a "Due Today" tag, and the habit's colour as
 * a dot at the right edge. The whole pill is the click target: only the name used to be, so
 * hovering lit a thin strip inside the card. Right-click edits, like a long press on the phone.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopHabitRow(
    habit: Habit,
    isDone: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    isDueToday: Boolean = false
) {
    val habitColor = resolveDisplayColor(habit.colorIndex, habit.colorArgb)
    Box(
        Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding, vertical = 4.dp)
            .crystalTileSurface()
            .clickable { onClick() }
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) { onEdit() }
    ) {
        Box(
            Modifier.align(Alignment.CenterEnd).padding(10.dp).size(10.dp).background(
                if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f) else habitColor,
                CircleShape
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundCheck(checked = isDone, onToggle = onToggle)
            Text(
                habit.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(20.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit habit",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (isDueToday && !isDone) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("Due Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * The "N done" line — a bare row, not a [CollapsibleSectionHeader], the same "quiet" affordance the
 * Android app uses so a finished habit doesn't get the same visual weight as an active one.
 */
@Composable
internal fun SectionFoldToggle(label: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onToggle)
            .padding(horizontal = PlannerMetrics.RowInset, vertical = 6.dp)
    ) {
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
