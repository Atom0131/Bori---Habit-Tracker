package com.apagon.rhythm.ui.todos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.components.crystalCheckboxColors
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.util.CollapsibleSectionHeader
import com.apagon.rhythm.ui.util.SectionHeaderTier
import com.apagon.rhythm.ui.util.getDueDateAsLocalDate
import androidx.compose.material.icons.Icons
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import com.apagon.rhythm.ui.util.RoundCheck
import org.koin.compose.koinInject
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.ui.habit.SectionFoldToggle
import com.apagon.rhythm.ui.util.SectionEmptyCard
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.data.model.TodoPriority
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.onClick
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.ExperimentalFoundationApi

// Stage 17e: To-dos is no longer a standalone sidebar screen — the real Android app has no
// separate To-dos destination at all; HabitListScreen.kt (the "Today" tab) composes TodoViewModel
// state directly alongside habits in one continuous list. This is now a LazyListScope extension
// appended after the habit sections in DesktopTodayScreen's list mode, instead of its own
// Scaffold+width-cap+LazyColumn screen. DesktopTodoRow (the per-todo card) is unchanged content,
// just no longer wrapped in its own screen shell.
// Stage 18: Android's real HabitListScreen.kt also renders its "todos" section through
// CollapsibleSectionHeader, using the exact same collapsedSections set the Daily/Weekly/Monthly
// habit sections use — everything starts folded closed, not just habits. Matches that here with
// its own expanded/onToggleExpanded pair (kept separate from the habit frequencies' Set<HabitFrequency>
// state rather than widening that type, since To-dos isn't a HabitFrequency).
// Stage 19e: the inline "New to-do" row used to sit here — removed now that the Today FAB
// (Stage 19c) is the single discoverable add entry point for both habits and to-dos, matching
// Android's HabitListScreen.kt (no inline todosSection add row there either, only the FAB).
/**
 * Port of Android's `todosSection` (`DayDetailView.kt`): "To-do List", the selected day's to-dos
 * (plus undated ones), then the overdue ones and the ones finished that day each folded behind a
 * quiet "N overdue" / "N done" line. Desktop Today used to list every pending to-do regardless of
 * the selected day.
 */
internal fun LazyListScope.todoSection(
    dueToday: List<Todo>,
    overdue: List<Todo>,
    completed: List<Todo>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    overdueExpanded: Boolean,
    onToggleOverdueExpanded: () -> Unit,
    completedExpanded: Boolean,
    onToggleCompletedExpanded: () -> Unit,
    onToggle: (Todo) -> Unit,
    onEdit: (Todo) -> Unit
) {
    item(key = "todos_header") {
        CollapsibleSectionHeader(
            title = "To-do List",
            expanded = expanded,
            onToggle = onToggleExpanded,
            tier = SectionHeaderTier.Primary
        )
    }
    if (!expanded) return

    if (dueToday.isEmpty() && overdue.isEmpty() && completed.isEmpty()) {
        item(key = "todos_empty") { SectionEmptyCard("Nothing to do here", horizontalPadding = DesktopLayout.screenPadding) }
    } else {
        items(dueToday, key = { "todo_${it.id}" }) { todo ->
            DesktopTodoRow(todo, onToggle = { onToggle(todo) }, onEdit = { onEdit(todo) })
        }
    }

    if (overdue.isNotEmpty()) {
        item(key = "todos_overdue_header") {
            SectionFoldToggle("${overdue.size} overdue", overdueExpanded, onToggleOverdueExpanded)
        }
        if (overdueExpanded) items(overdue, key = { "todo_overdue_${it.id}" }) { todo ->
            DesktopTodoRow(todo, onToggle = { onToggle(todo) }, onEdit = { onEdit(todo) })
        }
    }

    if (completed.isNotEmpty()) {
        item(key = "todos_completed_header") {
            SectionFoldToggle("${completed.size} done", completedExpanded, onToggleCompletedExpanded)
        }
        if (completedExpanded) items(completed, key = { "done_todo_${it.id}" }) { todo ->
            DesktopTodoRow(todo, onToggle = { onToggle(todo) }, onEdit = { onEdit(todo) })
        }
    }
}

private val todoPriorityColors = mapOf(
    TodoPriority.HIGH to Color(0xFFE53935),
    TodoPriority.MEDIUM to Color(0xFFF57C00),
    TodoPriority.LOW to Color(0xFF43A047)
)

private fun formatTime(hour: Int, minute: Int, is24Hour: Boolean): String =
    if (is24Hour) "%02d:%02d".format(hour, minute)
    else "%d:%02d %s".format(when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }, minute, if (hour < 12) "AM" else "PM")

/**
 * Port of Android's `TodoRow` (`TodoRowComposable.kt`), minus subtasks and swipe-to-delete: round
 * check, title (struck through once done), note, a due line in Android's wording and colours
 * ("Due Today", "Due Tomorrow", red "Overdue Oct 7", "Completed today"…), the H/M/L priority
 * badge and an edit pencil. Delete lives in the edit sheet, as on Android. The whole card opens
 * the editor; right-click does too.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DesktopTodoRow(todo: Todo, onToggle: () -> Unit, onEdit: () -> Unit) {
    val localeFormatting = koinInject<LocaleFormatting>()
    val is24Hour = remember(localeFormatting) { localeFormatting.is24HourFormat() }
    val today = remember { LocalDate.now() }
    val dueDate = remember(todo.dueDate) {
        todo.dueDate.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it.substringBefore(" ")) }.getOrNull() }
    }
    val dueTime = remember(todo.dueDate, is24Hour) {
        todo.dueDate.takeIf { it.contains(" ") }?.let {
            runCatching {
                val t = it.substringAfter(" ")
                formatTime(t.substringBefore(":").toInt(), t.substringAfter(":").toInt(), is24Hour)
            }.getOrNull()
        }
    }
    val isOverdue = dueDate != null && dueDate.isBefore(today) && !todo.isCompleted
    // Android maps an unknown priority to LOW; desktop's extra NONE does the same, so a to-do
    // reads identically on both.
    val priority = TodoPriority.entries.firstOrNull { it.name == todo.priority && it != TodoPriority.NONE } ?: TodoPriority.LOW
    val monthDay = remember { DateTimeFormatter.ofPattern("MMM d") }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = DesktopLayout.screenPadding, vertical = 4.dp)
            .crystalTileSurface()
            .clickable { onEdit() }
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) { onEdit() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundCheck(checked = todo.isCompleted, onToggle = onToggle)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                todo.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (todo.note.isNotBlank()) {
                Text(
                    todo.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (dueDate != null) {
                Spacer(Modifier.height(2.dp))
                val dateText = if (todo.isCompleted) {
                    when (val done = todo.completedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }) {
                        null -> "Completed"
                        today -> "Completed today"
                        today.minusDays(1) -> "Completed yesterday"
                        else -> "Completed ${done.format(monthDay)}"
                    }
                } else {
                    val base = when {
                        dueDate == today -> "Due Today"
                        dueDate == today.plusDays(1) -> "Due Tomorrow"
                        isOverdue -> "Overdue ${dueDate.format(monthDay)}"
                        else -> "Due ${dueDate.format(monthDay)}"
                    }
                    if (dueTime != null) "$base at $dueTime" else base
                }
                Text(
                    dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isOverdue -> MaterialTheme.colorScheme.error
                        dueDate == today || dueDate == today.plusDays(1) -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        val badgeColor = todoPriorityColors[priority] ?: MaterialTheme.colorScheme.surfaceContainerHighest
        Box(
            Modifier.padding(start = 8.dp).size(22.dp).background(badgeColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(priority.name.take(1), style = MaterialTheme.typography.labelSmall, color = badgeColor)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit to-do", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
