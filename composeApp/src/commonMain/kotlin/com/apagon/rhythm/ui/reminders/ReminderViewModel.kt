package com.apagon.rhythm.ui.reminders

import com.apagon.rhythm.platform.ReminderScheduling
import com.apagon.rhythm.core.time.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.apagon.rhythm.ui.util.REMINDER_INPUT_FMT
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
class ReminderViewModel constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduling
) : ViewModel() {

    private val allReminders = repository.getAllActiveReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingReminders = allReminders.map { list ->
        val now = LocalDateTime.now()
        list.filter { !it.isCompleted && parseDateTime(it.dateTime)?.isAfter(now) == true }
            .sortedBy { it.dateTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pastReminders = allReminders.map { list ->
        val now = LocalDateTime.now()
        list.filter { !it.isCompleted && parseDateTime(it.dateTime)?.isAfter(now) != true }
            .sortedByDescending { it.dateTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedReminders = allReminders.map { list ->
        list.filter { it.isCompleted }
            .sortedByDescending { it.dateTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReminder(title: String, note: String, dateTime: String, soundUri: String = "") {
        viewModelScope.launch {
            val id = repository.addReminder(Reminder(title = title, note = note, dateTime = dateTime, soundUri = soundUri))
            val reminder = Reminder(id = id, title = title, note = note, dateTime = dateTime, soundUri = soundUri)
            scheduler.scheduleOneShot(reminder)
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            scheduler.scheduleOneShot(reminder)
        }
    }

    fun toggleCompletion(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            scheduler.cancelOneShot(reminder.id)
        }
    }

    private fun parseDateTime(dateTime: String): LocalDateTime? = try {
        LocalDateTime.parse(dateTime, REMINDER_INPUT_FMT)
    } catch (e: Exception) {
        null
    }
}
