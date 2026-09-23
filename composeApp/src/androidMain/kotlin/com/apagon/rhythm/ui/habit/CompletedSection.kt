package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistProgress
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.ui.util.CollapsibleSectionHeader
import com.apagon.rhythm.ui.todos.TodoRow

/** Unified completed section for all item types. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun LazyListScope.completedSection(
    completedHabits: List<Habit>,
    completedTodos: List<Todo>,
    completedReminders: List<Reminder>,
    expanded: Boolean,
    onToggle: () -> Unit,
    todayChecklistProgress: Map<Long, ChecklistProgress>,
    onHabitToggle: (Habit) -> Unit,
    onHabitItemToggle: (Long, ChecklistItem, Boolean) -> Unit,
    onHabitArchive: (Habit) -> Unit,
    onHabitDelete: (Habit) -> Unit,
    onHabitEdit: (Habit) -> Unit,
    onHabitView: (Habit) -> Unit,
    onTodoToggle: (Todo) -> Unit,
    onTodoEdit: (Todo) -> Unit,
    onTodoDelete: (Todo) -> Unit,
    onReminderToggle: (Reminder) -> Unit,
    onReminderEdit: (Reminder) -> Unit,
    onReminderDelete: (Reminder) -> Unit
) {
    val total = completedHabits.size + completedTodos.size + completedReminders.size
    if (total == 0) return

    stickyHeader(key = "unified_completed_header") {
        CollapsibleSectionHeader(
            title = "Completed ($total)",
            expanded = expanded,
            onToggle = onToggle
        )
    }

    if (expanded) {
        items(completedHabits, key = { "done_habit_${it.id}" }) { habit ->
            HabitRow(
                habit = habit,
                isDone = true,
                checklistProgress = todayChecklistProgress[habit.id],
                onToggle = { onHabitToggle(habit) },
                onToggleItem = { item, isDone -> onHabitItemToggle(habit.id, item, isDone) },
                onArchive = { onHabitArchive(habit) },
                onDelete = { onHabitDelete(habit) },
                onEdit = { onHabitEdit(habit) },
                onView = { onHabitView(habit) },
                modifier = Modifier.animateItem()
            )
        }
        items(completedTodos, key = { "done_todo_${it.id}" }) { todo ->
            TodoRow(
                todo = todo,
                onToggle = { onTodoToggle(todo) },
                onEdit = { onTodoEdit(todo) },
                onDelete = { onTodoDelete(todo) },
                modifier = Modifier.animateItem()
            )
        }
        items(completedReminders, key = { "done_reminder_${it.id}" }) { reminder ->
            ReminderRow(
                reminder = reminder,
                onToggle = { onReminderToggle(reminder) },
                onEdit = { onReminderEdit(reminder) },
                onDelete = { onReminderDelete(reminder) },
                modifier = Modifier.animateItem()
            )
        }
    }
}
