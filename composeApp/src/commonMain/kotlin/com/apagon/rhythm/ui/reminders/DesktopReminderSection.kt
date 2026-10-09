package com.apagon.rhythm.ui.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.ui.calendar.DesktopReminderRow
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.util.CollapsibleSectionHeader
import com.apagon.rhythm.ui.util.SectionHeaderTier
import com.apagon.rhythm.ui.util.SectionEmptyCard

/**
 * Filters a reminder list down to the ones due on [dateStr] (ISO `yyyy-MM-dd`) — the same
 * substring-match [DesktopCalendarScreen]'s day-detail already did inline; extracted here so
 * Stage 19g's Today list-mode section can reuse it instead of re-deriving it.
 */
fun remindersForDate(reminders: List<Reminder>, dateStr: String): List<Reminder> =
    reminders.filter { it.dateTime.substringBefore(" ") == dateStr }

/**
 * Port of Android's `remindersSection` (`ui/habit/DayDetailView.kt`) for desktop's Today
 * list-mode — previously Reminders only existed buried in Today's separate Calendar-mode
 * day-detail, unlike Android where they're always inline on the list, between the habit
 * sections and To-dos (Stage 19g). Desktop-styled rather than a literal port: follows the
 * established [com.apagon.rhythm.ui.todos.todoSection] shape (CollapsibleSectionHeader,
 * flat list, no overdue/grouped subdivision) instead of Android's heavier grouped version,
 * reusing [DesktopReminderRow] rather than duplicating it.
 */
internal fun LazyListScope.remindersSection(
    reminders: List<Reminder>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggle: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit
) {
    item(key = "reminders_header") {
        CollapsibleSectionHeader(
            title = "Reminders",
            expanded = expanded,
            onToggle = onToggleExpanded,
            tier = SectionHeaderTier.Primary
        )
    }
    if (!expanded) return

    if (reminders.isEmpty()) {
        item(key = "reminders_empty") { SectionEmptyCard("No reminders for this day.", horizontalPadding = DesktopLayout.screenPadding) }
    } else {
        items(reminders, key = { "today_reminder_${it.id}" }) { reminder ->
            Box(Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding, vertical = 4.dp)) {
                DesktopReminderRow(reminder, onToggle = { onToggle(reminder) }, onDelete = { onDelete(reminder) })
            }
        }
    }
}
