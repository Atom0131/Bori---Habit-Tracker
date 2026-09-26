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

    /**
     * Schedules the OS-level completion alert for a running Timer. Android
     * actual: AlarmManager (RTC_WAKEUP) + a foreground service so the
     * countdown keeps firing even if the app is backgrounded/killed. Desktop
     * actual: no-op — DesktopAlarmClockService (Stage 12) polls the DB
     * directly and doesn't need a per-item registration.
     */
    fun scheduleTimerCompletion(timerId: Long, endTimeMillis: Long, label: String)
    fun cancelTimerCompletion(timerId: Long)
}

/**
 * Pushes fresh data to home-screen widgets after data changes.
 * Android actual: Glance updateAll. iOS: no-op until WidgetKit support lands.
 */
interface WidgetRefresher {
    suspend fun refreshAll()
}

/**
 * Whether the platform's locale/user setting prefers 24-hour time display.
 * Android actual: android.text.format.DateFormat.is24HourFormat(context).
 * Desktop actual: derived from java.text.DateFormat's locale pattern.
 */
interface LocaleFormatting {
    fun is24HourFormat(): Boolean
}

/**
 * Starts/stops a repeating vibration pattern for a high-priority alert
 * (alarm/timer/reminder firing). Android actual: the device vibrator
 * (ui/util/Extensions.kt's AlertVibrator). Desktop actual: no-op — no
 * standard Linux desktop haptics API.
 */
interface HapticAlerter {
    fun start(patternId: String = "default")
    fun stop()
}
