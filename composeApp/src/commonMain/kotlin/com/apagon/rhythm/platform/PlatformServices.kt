package com.apagon.rhythm.platform

import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Todo

/**
 * Schedules exact-time reminders/alarms with the OS.
 * Android actual: AlarmManager (notifications/ReminderScheduler).
 * iOS actual: UNUserNotificationCenter local notifications.
 */
interface ReminderScheduling {
    fun scheduleReminder(habit: Habit)
    fun cancelReminder(habitId: Long)
    fun scheduleOneShot(reminder: Reminder)
    fun cancelOneShot(reminderId: Long)
    fun scheduleAlarm(alarm: Alarm)
    fun cancelAlarm(alarmId: Long)
    fun scheduleTodo(todo: Todo)
    fun cancelTodo(todoId: Long)
}

/**
 * Pushes fresh data to home-screen widgets after data changes.
 * Android actual: Glance updateAll. iOS: no-op until WidgetKit support lands.
 */
interface WidgetRefresher {
    suspend fun refreshAll()
}
