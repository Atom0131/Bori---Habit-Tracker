package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.ui.components.crystalCheckboxColors
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.util.CollapsibleSectionHeader
import com.apagon.rhythm.ui.util.PlannerMetrics
import com.apagon.rhythm.ui.util.SectionHeaderTier

/**
 * Desktop port of the Android app's `habitFrequencySection`/`SectionFoldToggle`
 * (`ui/habit/HabitFrequencySection.kt`) — one frequency's heading, its outstanding rows, and its
 * finished ones folded behind a quiet "N done" line, so completed habits stay with the frequency
 * they came from instead of a separate global Completed bucket.
 *
 * Deliberately narrower than the Android original: no checklist items, no swipe-to-archive/delete,
 * no per-habit edit sheet — none of those are wired up on desktop today, so this only ports the
 * grouping/fold-toggle *shape*, reusing desktop's existing simple checkbox+name row.
 */
internal fun LazyListScope.habitFrequencySection(
    frequency: HabitFrequency,
    pending: List<Habit>,
    completed: List<Habit>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    doneExpanded: Boolean,
    onToggleDone: () -> Unit,
    onToggleCompletion: (habitId: Long, isDone: Boolean) -> Unit,
    onView: (Habit) -> Unit
) {
    if (pending.isEmpty() && completed.isEmpty()) return

    val sectionKey = frequency.name

    item(key = "${sectionKey}_header") {
        CollapsibleSectionHeader(
            title = frequency.sectionTitle(),
            expanded = expanded,
            onToggle = onToggleExpanded,
            tier = SectionHeaderTier.Primary
        )
    }

    if (!expanded) return

    items(pending, key = { it.id }) { habit ->
        DesktopHabitRow(
            habit = habit,
            isDone = false,
            onToggle = { onToggleCompletion(habit.id, false) },
            onClick = { onView(habit) }
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
                onClick = { onView(habit) }
            )
        }
    }
}

private fun HabitFrequency.sectionTitle(): String = when (this) {
    HabitFrequency.DAILY -> "Daily Habits"
    HabitFrequency.WEEKLY -> "Weekly Habits"
    HabitFrequency.MONTHLY -> "Monthly Habits"
}

@Composable
private fun DesktopHabitRow(habit: Habit, isDone: Boolean, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .crystalTileSurface().padding(horizontal = PlannerMetrics.RowInset, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isDone, onCheckedChange = { onToggle() }, colors = crystalCheckboxColors())
        Text(
            habit.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).clickable { onClick() }
        )
    }
}

/**
 * The "N done" line — a bare row, not a [CollapsibleSectionHeader], the same "quiet" affordance the
 * Android app uses so a finished habit doesn't get the same visual weight as an active one.
 */
@Composable
private fun SectionFoldToggle(label: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onToggle)
            .padding(horizontal = PlannerMetrics.RowInset, vertical = 6.dp)
    ) {
        Text(
            text = if (expanded) "▴" else "▾",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
