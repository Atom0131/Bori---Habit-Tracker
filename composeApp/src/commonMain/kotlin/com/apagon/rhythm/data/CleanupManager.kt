package com.apagon.rhythm.data

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class CleanupManager constructor(
    private val habitRepository: HabitRepository,
    private val todoRepository: TodoRepository,
    private val reminderRepository: ReminderRepository,
    private val eventRepository: CalendarEventRepository,
    private val alarmRepository: AlarmRepository,
    private val timerRepository: TimerRepository,
    private val notesRepository: NotesRepository,
    private val journalRepository: JournalRepository
) {
    suspend fun purgeAll() = withContext(Dispatchers.Default) {
        val fourteenDaysAgo = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)
        
        habitRepository.purgeOldDeletedItems(fourteenDaysAgo)
        todoRepository.purgeOldDeletedItems(fourteenDaysAgo)
        reminderRepository.purgeOldDeletedItems(fourteenDaysAgo)
        eventRepository.purgeOldDeletedItems(fourteenDaysAgo)
        alarmRepository.purgeOldDeletedItems(fourteenDaysAgo)
        timerRepository.purgeOldDeletedItems(fourteenDaysAgo)
        notesRepository.purgeOldDeletedItems(fourteenDaysAgo)
        journalRepository.purgeOldDeletedItems(fourteenDaysAgo)
    }
}
