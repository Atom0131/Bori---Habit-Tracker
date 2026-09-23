package com.apagon.rhythm.ui.todos

import com.apagon.rhythm.platform.ReminderScheduling
import com.apagon.rhythm.platform.WidgetRefresher
import com.apagon.rhythm.core.time.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.TodoPriority
import com.apagon.rhythm.data.repository.TodoRepository
import com.apagon.rhythm.data.preferences.ThemePreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.apagon.rhythm.ui.util.getDueDateAsLocalDate
import com.apagon.rhythm.ui.util.toDayStartEndMillis
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModel constructor(
    private val repository: TodoRepository,
    private val themePreferences: ThemePreferences,
    private val scheduler: ReminderScheduling,
    private val widgetRefresher: WidgetRefresher
) : ViewModel() {

    private val _ticker = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(10 * 60 * 1000L) // Refresh every 10 mins
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val todayFlow = _ticker.map { LocalDate.now() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now())

    val isPro: StateFlow<Boolean> = themePreferences.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val priorityOrder = mapOf(
        TodoPriority.HIGH   to 0,
        TodoPriority.MEDIUM to 1,
        TodoPriority.LOW    to 2,
        TodoPriority.NONE   to 3
    )

    private fun safePriority(name: String): TodoPriority =
        TodoPriority.entries.firstOrNull { it.name == name } ?: TodoPriority.NONE

    val pendingTodos: StateFlow<List<Todo>> = repository.getPendingTodos()
        .map { list ->
            list.sortedWith(
                compareBy(
                    { priorityOrder[safePriority(it.priority)] },
                    { it.dueDate.ifEmpty { "9999-99-99" } }
                )
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedTodos: StateFlow<List<Todo>> = todayFlow.flatMapLatest { today: LocalDate ->
        val (dayStart, dayEnd) = today.toDayStartEndMillis()
        repository.getCompletedTodosForDay(dayStart, dayEnd)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val overdueTodos: StateFlow<List<Todo>> = kotlinx.coroutines.flow.combine(pendingTodos, todayFlow) { list, today ->
        list.filter { todo ->
            todo.getDueDateAsLocalDate()?.isBefore(today) == true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Only today's dated todos and no-date todos (excludes future-dated)
    val dueTodayTodos: StateFlow<List<Todo>> = kotlinx.coroutines.flow.combine(pendingTodos, todayFlow) { list, today ->
        list.filter { todo ->
            todo.getDueDateAsLocalDate() == today
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTodo(title: String, note: String, dueDate: String, priority: TodoPriority, iconIndex: Int = 0, soundUri: String = "", vibrationPatternId: String = "default") {
        viewModelScope.launch {
            val todo = Todo(
                title = title.trim(),
                note = note.trim(),
                dueDate = dueDate,
                priority = priority.name,
                iconIndex = iconIndex,
                soundUri = soundUri,
                vibrationPatternId = vibrationPatternId
            )
            val id = repository.addTodo(todo)
            if (dueDate.contains(" ")) {
                scheduler.scheduleTodo(todo.copy(id = id))
            }
            widgetRefresher.refreshAll()
        }
    }

    fun toggleCompletion(todo: Todo) {
        viewModelScope.launch {
            val completing = !todo.isCompleted
            val updated = todo.copy(
                isCompleted = completing,
                completedAt = if (completing) System.currentTimeMillis() else null
            )
            repository.updateTodo(updated)
            if (completing) scheduler.cancelTodo(todo.id)
            widgetRefresher.refreshAll()
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            scheduler.cancelTodo(todo.id)
            repository.deleteTodo(todo)
            widgetRefresher.refreshAll()
        }
    }

    fun updateTodo(todo: Todo, title: String, note: String, dueDate: String, priority: TodoPriority, iconIndex: Int = todo.iconIndex, soundUri: String = todo.soundUri, vibrationPatternId: String = todo.vibrationPatternId) {
        viewModelScope.launch {
            val updated = todo.copy(
                title = title.trim(),
                note = note.trim(),
                dueDate = dueDate,
                priority = priority.name,
                iconIndex = iconIndex,
                soundUri = soundUri,
                vibrationPatternId = vibrationPatternId
            )
            repository.updateTodo(updated)
            scheduler.cancelTodo(todo.id)
            if (dueDate.contains(" ")) {
                scheduler.scheduleTodo(updated)
            }
        }
    }
}
