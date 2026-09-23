package com.apagon.rhythm.widget

import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.ReminderRepository
import com.apagon.rhythm.data.repository.TimerRepository
import com.apagon.rhythm.data.repository.TodoRepository
import org.koin.core.context.GlobalContext

/**
 * Koin-backed service locator for Glance widgets and their receivers, which run
 * outside any Activity/ViewModel scope (replaces the former Hilt @EntryPoint).
 */
class WidgetEntryPoint {
    private val koin get() = GlobalContext.get()

    fun habitRepository(): HabitRepository = koin.get()
    fun todoRepository(): TodoRepository = koin.get()
    fun timerRepository(): TimerRepository = koin.get()
    fun reminderRepository(): ReminderRepository = koin.get()
    fun themePreferences(): ThemePreferences = koin.get()
}
