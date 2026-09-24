package com.apagon.rhythm.ui.deleted

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DesktopDeletedItems(
    val habits: List<Habit> = emptyList(),
    val todos: List<Todo> = emptyList()
) {
    val isEmpty: Boolean get() = habits.isEmpty() && todos.isEmpty()
}

/**
 * A deliberately narrow ViewModel, same reasoning as [com.apagon.rhythm.ui.habit.DesktopHabitViewModel]:
 * the full [RecentlyDeletedViewModel] takes 8 repositories spanning every
 * entity type (Reminders, Calendar, Alarms, Timers, Notes, Journal) —
 * verticals that don't exist on desktop yet. This covers just Habit and
 * Todo, the two that do, and grows alongside DesktopHabitDatabase as later
 * stages add more entities rather than wiring 6 repositories with nothing
 * behind them.
 */
class DesktopRecentlyDeletedViewModel(
    private val habitRepository: HabitRepository,
    private val todoRepository: TodoRepository
) : ViewModel() {

    val deletedItems: StateFlow<DesktopDeletedItems> = combine(
        habitRepository.getDeletedHabits(),
        todoRepository.getDeletedTodos()
    ) { habits, todos ->
        DesktopDeletedItems(habits = habits, todos = todos)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DesktopDeletedItems())

    fun restoreHabit(habit: Habit) {
        viewModelScope.launch { habitRepository.updateHabit(habit.copy(deletedAt = null)) }
    }

    fun hardDeleteHabit(habit: Habit) {
        viewModelScope.launch { habitRepository.hardDeleteHabit(habit) }
    }

    fun restoreTodo(todo: Todo) {
        viewModelScope.launch { todoRepository.updateTodo(todo.copy(deletedAt = null)) }
    }

    fun hardDeleteTodo(todo: Todo) {
        viewModelScope.launch { todoRepository.hardDeleteTodo(todo) }
    }
}
