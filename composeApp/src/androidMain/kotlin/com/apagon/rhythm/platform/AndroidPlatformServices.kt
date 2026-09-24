package com.apagon.rhythm.platform

import android.content.Context
import android.text.format.DateFormat
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.notifications.ReminderScheduler
import com.apagon.rhythm.ui.util.AlertVibrator
import com.apagon.rhythm.widget.refreshAllWidgets

class AndroidReminderScheduling(private val context: Context) : ReminderScheduling {
    override fun scheduleReminder(habit: Habit) = ReminderScheduler.scheduleReminder(context, habit)
    override fun cancelReminder(habitId: Long) = ReminderScheduler.cancelReminder(context, habitId)
    override fun scheduleOneShot(reminder: Reminder) = ReminderScheduler.scheduleOneShot(context, reminder)
    override fun cancelOneShot(reminderId: Long) = ReminderScheduler.cancelOneShot(context, reminderId)
    override fun scheduleAlarm(alarm: Alarm) = ReminderScheduler.scheduleAlarm(context, alarm)
    override fun cancelAlarm(alarmId: Long) = ReminderScheduler.cancelAlarm(context, alarmId)
    override fun scheduleTodo(todo: Todo) = ReminderScheduler.scheduleTodo(context, todo)
    override fun cancelTodo(todoId: Long) = ReminderScheduler.cancelTodo(context, todoId)
}

class AndroidWidgetRefresher(private val context: Context) : WidgetRefresher {
    override suspend fun refreshAll() = refreshAllWidgets(context)
}

class AndroidLocaleFormatting(private val context: Context) : LocaleFormatting {
    override fun is24HourFormat(): Boolean = DateFormat.is24HourFormat(context)
}

class AndroidHapticAlerter(private val context: Context) : HapticAlerter {
    override fun start(patternId: String) = AlertVibrator.start(context, patternId)
    override fun stop() = AlertVibrator.stop(context)
}
