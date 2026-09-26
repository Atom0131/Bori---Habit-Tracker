package com.apagon.rhythm.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.repository.TimerRepository
import com.apagon.rhythm.notifications.ReminderScheduler
import com.apagon.rhythm.notifications.TimerCompletionReceiver
import com.apagon.rhythm.notifications.TimerForegroundService
import com.apagon.rhythm.ui.util.AlertVibrator
import com.apagon.rhythm.widget.refreshAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AndroidReminderScheduling(
    private val context: Context,
    private val timerRepository: TimerRepository
) : ReminderScheduling {
    override fun scheduleReminder(habit: Habit) = ReminderScheduler.scheduleReminder(context, habit)
    override fun cancelReminder(habitId: Long) = ReminderScheduler.cancelReminder(context, habitId)
    override fun scheduleOneShot(reminder: Reminder) = ReminderScheduler.scheduleOneShot(context, reminder)
    override fun cancelOneShot(reminderId: Long) = ReminderScheduler.cancelOneShot(context, reminderId)
    override fun scheduleAlarm(alarm: Alarm) = ReminderScheduler.scheduleAlarm(context, alarm)
    override fun cancelAlarm(alarmId: Long) = ReminderScheduler.cancelAlarm(context, alarmId)
    override fun scheduleTodo(todo: Todo) = ReminderScheduler.scheduleTodo(context, todo)
    override fun cancelTodo(todoId: Long) = ReminderScheduler.cancelTodo(context, todoId)

    // Extracted from androidMain's original TimerViewModel (moved to commonMain
    // in Stage 12) — same AlarmManager.setExactAndAllowWhileIdle + foreground
    // service behavior, just relocated behind ReminderScheduling so the
    // commonMain ViewModel no longer touches Context/AlarmManager directly.
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun scheduleTimerCompletion(timerId: Long, endTimeMillis: Long, label: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, completionPendingIntent(timerId))
        context.startService(TimerForegroundService.startIntent(context, endTimeMillis, label))
    }

    override fun cancelTimerCompletion(timerId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(completionPendingIntent(timerId))
        scope.launch {
            val anyRunning = timerRepository.getAllTimers().first().any { it.endTimeMillis > 0 }
            if (!anyRunning) {
                context.startService(TimerForegroundService.stopIntent(context))
            }
        }
    }

    private fun completionPendingIntent(timerId: Long): PendingIntent {
        val intent = Intent(context, TimerCompletionReceiver::class.java).apply {
            putExtra(TimerCompletionReceiver.EXTRA_TIMER_ID, timerId)
        }
        return PendingIntent.getBroadcast(
            context, timerId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
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
