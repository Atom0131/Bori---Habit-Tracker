package com.apagon.rhythm.notifications
import com.apagon.rhythm.core.time.*
import kotlinx.datetime.LocalDate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.ui.util.REMINDER_INPUT_FMT
import com.apagon.rhythm.ui.util.isScheduledForDate
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import com.apagon.rhythm.core.time.ZoneId
import java.util.Calendar

object ReminderScheduler {

    internal const val REMINDER_ID_OFFSET = 500_000L
    internal const val ALARM_ID_OFFSET    = 700_000L
    internal const val EVENT_ID_OFFSET    = 900_000L
    internal const val TODO_ID_OFFSET     = 1_100_000L
    private const val SNOOZE_ID_OFFSET    = 300_000L

    fun scheduleReminder(context: Context, habit: Habit) {
        cancelReminder(context, habit.id)
        val reminderTime = habit.reminderTime ?: return
        val nextTrigger = nextTriggerCalendarForHabit(habit, reminderTime) ?: return
        scheduleAt(
            context, habit.id, habit.name, reminderTime,
            nextTrigger,
            type = TYPE_HABIT,
            soundUri = habit.soundUri,
            vibrationPatternId = habit.vibrationPatternId
        )
    }

    fun cancelReminder(context: Context, habitId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(
            context, habitId, "", "", TYPE_HABIT,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    fun scheduleAll(context: Context, habits: List<Habit>) {
        habits.forEach { scheduleReminder(context, it) }
    }



    fun scheduleOneShot(context: Context, reminder: Reminder) {
        cancelOneShot(context, reminder.id)
        val calendar = parseDateTimeToCalendar(reminder.dateTime) ?: return
        if (calendar.timeInMillis <= System.currentTimeMillis()) return
        scheduleAt(
            context,
            REMINDER_ID_OFFSET + reminder.id,
            reminder.title,
            reminder.note,
            calendar,
            type = TYPE_REMINDER,
            soundUri = reminder.soundUri
        )
    }

    fun cancelOneShot(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(
            context, REMINDER_ID_OFFSET + reminderId, "", "", TYPE_REMINDER,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    fun scheduleAllOneShot(context: Context, reminders: List<Reminder>) {
        reminders.forEach { scheduleOneShot(context, it) }
    }

    fun scheduleAlarm(context: Context, alarm: Alarm) {
        cancelAlarm(context, alarm.id)
        if (!alarm.isEnabled) return
        val calendar = nextAlarmCalendar(alarm.hour, alarm.minute, alarm.repeatDaysMask) ?: return
        scheduleAt(
            context,
            ALARM_ID_OFFSET + alarm.id,
            alarm.label,
            "",
            calendar,
            type = TYPE_ALARM,
            soundUri = alarm.soundUri,
            vibrationPatternId = alarm.vibrationPatternId,
            repeatDays = alarm.repeatDaysMask,
            hour = alarm.hour,
            minute = alarm.minute
        )
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(
            context, ALARM_ID_OFFSET + alarmId, "", "", TYPE_ALARM,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    fun scheduleTodo(context: Context, todo: Todo) {
        cancelTodo(context, todo.id)
        val space = todo.dueDate.indexOf(' ')
        if (space < 0) return
        val timePart = todo.dueDate.substring(space + 1).substringBefore(" -")
        val calendar = runCatching {
            val ld = LocalDate.parse(todo.dueDate.substring(0, space))
            val (h, m) = parseTime(timePart)
            Calendar.getInstance().apply {
                set(ld.year, ld.monthValue - 1, ld.dayOfMonth, h, m, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }.getOrNull() ?: return
        if (calendar.timeInMillis <= System.currentTimeMillis()) return
        scheduleAt(context, TODO_ID_OFFSET + todo.id, todo.title, todo.note, calendar, type = TYPE_TODO, soundUri = todo.soundUri, vibrationPatternId = todo.vibrationPatternId)
    }

    fun cancelTodo(context: Context, todoId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(
            context, TODO_ID_OFFSET + todoId, "", "", TYPE_TODO,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    fun scheduleSnooze(
        context: Context,
        type: String,
        id: Long,
        name: String,
        extra: String,
        soundUri: String = "",
        vibrationPatternId: String = "default",
        repeatDays: Int = 0,
        hour: Int = 0,
        minute: Int = 0
    ) {
        val snoozeMillis = System.currentTimeMillis() + 5 * 60 * 1000L
        val calendar = Calendar.getInstance().apply { timeInMillis = snoozeMillis }
        scheduleAt(context, id, name, extra, calendar, type, soundUri, vibrationPatternId, repeatDays, hour, minute)
    }

    private fun scheduleAt(
        context: Context,
        id: Long,
        name: String,
        extra: String,
        calendar: Calendar,
        type: String,
        soundUri: String = "",
        vibrationPatternId: String = "default",
        repeatDays: Int = 0,
        hour: Int = 0,
        minute: Int = 0
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(
            context, id, name, extra, type,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            soundUri = soundUri,
            vibrationPatternId = vibrationPatternId,
            repeatDays = repeatDays,
            hour = hour,
            minute = minute
        )
        
        if (type == TYPE_ALARM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
            } else {
                val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pi)
                alarmManager.setAlarmClock(info, pi)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
            }
        }
    }

    private fun nextTriggerCalendarForHabit(habit: Habit, reminderTime: String): Calendar? {
        val (hour, minute) = parseTime(reminderTime)
        var date = LocalDate.now()
        val now = Calendar.getInstance()
        for (i in 0..62) {
            if (habit.isScheduledForDate(date)) {
                val candidate = Calendar.getInstance().apply {
                    set(date.year, date.monthValue - 1, date.dayOfMonth, hour, minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (candidate.timeInMillis > now.timeInMillis) {
                    return candidate
                }
            }
            date = date.plusDays(1)
        }
        return null
    }

    private fun parseDateTimeToCalendar(dateTime: String): Calendar? {
        return try {
            val ldt = LocalDateTime.parse(dateTime, REMINDER_INPUT_FMT)
            val millis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            Calendar.getInstance().apply { timeInMillis = millis }
        } catch (e: Exception) {
            null
        }
    }

    private fun nextAlarmCalendar(hour: Int, minute: Int, repeatDays: Int): Calendar? {
        val now = Calendar.getInstance()
        val calDayMap = mapOf(
            0 to Calendar.SUNDAY,
            1 to Calendar.MONDAY,
            2 to Calendar.TUESDAY,
            3 to Calendar.WEDNESDAY,
            4 to Calendar.THURSDAY,
            5 to Calendar.FRIDAY,
            6 to Calendar.SATURDAY
        )
        for (offset in 0..7) { // Check up to 7 days ahead (0..7) to catch same-day-next-week
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.timeInMillis <= now.timeInMillis) continue
            
            // If it's a one-off (no repeats), return the very next occurrence
            if (repeatDays == 0) return candidate
            
            // User requested: automatically go off today if the time is upcoming, 
            // even if today isn't in the repeat mask.
            if (offset == 0) return candidate

            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            val bit = calDayMap.entries.firstOrNull { it.value == dayOfWeek }?.key ?: continue
            if (repeatDays and (1 shl bit) != 0) return candidate
        }
        return null
    }

    private fun buildPendingIntent(
        context: Context,
        id: Long,
        name: String,
        extra: String,
        type: String,
        flags: Int,
        soundUri: String = "",
        vibrationPatternId: String = "default",
        repeatDays: Int = 0,
        hour: Int = 0,
        minute: Int = 0
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TYPE, type)
            if (type == ReminderReceiver.TYPE_HABIT) {
                putExtra(ReminderReceiver.EXTRA_HABIT_ID, id)
                putExtra(ReminderReceiver.EXTRA_HABIT_NAME, name)
                putExtra(ReminderReceiver.EXTRA_REMINDER_TIME, extra)
                putExtra(ReminderReceiver.EXTRA_SOUND_URI, soundUri)
                putExtra(ReminderReceiver.EXTRA_VIBRATION_PATTERN_ID, vibrationPatternId)
            } else if (type == ReminderReceiver.TYPE_ALARM) {
                val baseId = id - ALARM_ID_OFFSET
                putExtra(ReminderReceiver.EXTRA_ALARM_ID, baseId)
                putExtra(ReminderReceiver.EXTRA_ALARM_LABEL, name)
                putExtra(ReminderReceiver.EXTRA_SOUND_URI, soundUri)
                putExtra(ReminderReceiver.EXTRA_VIBRATION_PATTERN_ID, vibrationPatternId)
                putExtra(ReminderReceiver.EXTRA_REPEAT_DAYS, repeatDays)
                putExtra(ReminderReceiver.EXTRA_ALARM_HOUR, hour)
                putExtra(ReminderReceiver.EXTRA_ALARM_MINUTE, minute)
            } else {
                val baseId = when (type) {
                    ReminderReceiver.TYPE_REMINDER -> id - REMINDER_ID_OFFSET
                    ReminderReceiver.TYPE_EVENT -> id - EVENT_ID_OFFSET
                    ReminderReceiver.TYPE_TODO -> id - TODO_ID_OFFSET
                    else -> id
                }
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, baseId)
                putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, name)
                putExtra(ReminderReceiver.EXTRA_REMINDER_NOTE, extra)
                putExtra(ReminderReceiver.EXTRA_SOUND_URI, soundUri)
                putExtra(ReminderReceiver.EXTRA_VIBRATION_PATTERN_ID, vibrationPatternId)
            }
        }
        return PendingIntent.getBroadcast(context, (id % Int.MAX_VALUE).toInt(), intent, flags)
    }

    internal fun parseTime(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        if (parts.size < 2) return 0 to 0
        val hour = parts[0].toIntOrNull() ?: 0
        val minute = parts[1].toIntOrNull() ?: 0
        return hour to minute
    }
}
