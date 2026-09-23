package com.apagon.rhythm.ui.deleted

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.*
import com.apagon.rhythm.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DeletedItems(
    val habits: List<Habit> = emptyList(),
    val todos: List<Todo> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val events: List<CalendarEvent> = emptyList(),
    val alarms: List<Alarm> = emptyList(),
    val timers: List<Timer> = emptyList(),
    val notebooks: List<Notebook> = emptyList(),
    val notes: List<Note> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList()
) {
    val isEmpty: Boolean get() = habits.isEmpty() && todos.isEmpty() && reminders.isEmpty() && 
            events.isEmpty() && alarms.isEmpty() && timers.isEmpty() && 
            notebooks.isEmpty() && notes.isEmpty() && journalEntries.isEmpty()
}
class RecentlyDeletedViewModel constructor(
    private val habitRepository: HabitRepository,
    private val todoRepository: TodoRepository,
    private val reminderRepository: ReminderRepository,
    private val eventRepository: CalendarEventRepository,
    private val alarmRepository: AlarmRepository,
    private val timerRepository: TimerRepository,
    private val notesRepository: NotesRepository,
    private val journalRepository: JournalRepository
) : ViewModel() {

    val deletedItems: StateFlow<DeletedItems> = combine(
        habitRepository.getDeletedHabits(),
        todoRepository.getDeletedTodos(),
        reminderRepository.getDeletedReminders(),
        eventRepository.getDeletedEvents(),
        alarmRepository.getDeletedAlarms(),
        timerRepository.getDeletedTimers(),
        notesRepository.getDeletedNotebooks(),
        notesRepository.getDeletedNotes(),
        journalRepository.getDeletedEntries()
    ) { flows ->
        DeletedItems(
            habits = flows[0] as List<Habit>,
            todos = flows[1] as List<Todo>,
            reminders = flows[2] as List<Reminder>,
            events = flows[3] as List<CalendarEvent>,
            alarms = flows[4] as List<Alarm>,
            timers = flows[5] as List<Timer>,
            notebooks = flows[6] as List<Notebook>,
            notes = flows[7] as List<Note>,
            journalEntries = flows[8] as List<JournalEntry>
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeletedItems())

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

    fun restoreReminder(reminder: Reminder) {
        viewModelScope.launch { reminderRepository.updateReminder(reminder.copy(deletedAt = null)) }
    }

    fun hardDeleteReminder(reminder: Reminder) {
        viewModelScope.launch { reminderRepository.hardDeleteReminder(reminder) }
    }

    fun restoreEvent(event: CalendarEvent) {
        viewModelScope.launch { eventRepository.updateEvent(event.copy(deletedAt = null)) }
    }

    fun hardDeleteEvent(event: CalendarEvent) {
        viewModelScope.launch { eventRepository.hardDeleteEvent(event) }
    }

    fun restoreAlarm(alarm: Alarm) {
        viewModelScope.launch { alarmRepository.updateAlarm(alarm.copy(deletedAt = null)) }
    }

    fun hardDeleteAlarm(alarm: Alarm) {
        viewModelScope.launch { alarmRepository.hardDeleteAlarm(alarm) }
    }

    fun restoreTimer(timer: Timer) {
        viewModelScope.launch { timerRepository.updateTimer(timer.copy(deletedAt = null)) }
    }

    fun hardDeleteTimer(timer: Timer) {
        viewModelScope.launch { timerRepository.hardDeleteTimer(timer) }
    }

    fun restoreNotebook(notebook: Notebook) {
        viewModelScope.launch { notesRepository.updateNotebook(notebook.copy(deletedAt = null)) }
    }

    fun hardDeleteNotebook(notebook: Notebook) {
        viewModelScope.launch { notesRepository.hardDeleteNotebook(notebook) }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch { notesRepository.updateNote(note.copy(deletedAt = null)) }
    }

    fun hardDeleteNote(note: Note) {
        viewModelScope.launch { notesRepository.hardDeleteNote(note) }
    }

    fun restoreJournalEntry(entry: JournalEntry) {
        viewModelScope.launch { journalRepository.updateEntry(entry.copy(deletedAt = null)) }
    }

    fun hardDeleteJournalEntry(entry: JournalEntry) {
        viewModelScope.launch { journalRepository.hardDeleteEntry(entry) }
    }
}
