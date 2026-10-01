package com.apagon.rhythm.ui.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.util.CollapsibleSectionHeader
import com.apagon.rhythm.ui.util.SectionHeaderTier

/**
 * Port of Android's `eventsSection` (`ui/habit/DayDetailView.kt`) for desktop's Today list-mode —
 * same reasoning as [com.apagon.rhythm.ui.reminders.remindersSection]: previously only reachable
 * via Calendar mode's day-detail, now always inline between the habit sections and To-dos
 * (Stage 19g), following [com.apagon.rhythm.ui.todos.todoSection]'s shape and reusing
 * [CalendarEventRow] rather than duplicating it.
 */
internal fun LazyListScope.eventsSection(
    events: List<CalendarEvent>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEdit: (CalendarEvent) -> Unit,
    onDelete: (CalendarEvent) -> Unit
) {
    item(key = "events_header") {
        CollapsibleSectionHeader(
            title = "Events",
            expanded = expanded,
            onToggle = onToggleExpanded,
            tier = SectionHeaderTier.Primary
        )
    }
    if (!expanded) return

    if (events.isEmpty()) {
        item(key = "events_empty") {
            Text(
                "No events for this day.",
                modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding, vertical = DesktopLayout.itemSpacing)
            )
        }
    } else {
        items(events, key = { "today_event_${it.id}" }) { event ->
            Box(Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding, vertical = 4.dp)) {
                CalendarEventRow(event, onEdit = { onEdit(event) }, onDelete = { onDelete(event) })
            }
        }
    }
}
