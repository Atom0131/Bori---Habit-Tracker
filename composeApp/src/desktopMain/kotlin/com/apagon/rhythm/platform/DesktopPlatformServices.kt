package com.apagon.rhythm.platform

import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Todo
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

// Desktop actual for WidgetRefresher — there's no home-screen widget system
// on Linux desktop, so this is a deliberate no-op (matches the interface's
// own doc comment: "iOS: no-op until WidgetKit support lands").
class DesktopWidgetRefresher : WidgetRefresher {
    override suspend fun refreshAll() {}
}

// Desktop actual for LocaleFormatting. There's no android.text.format on the
// JVM, so this reads java.text.DateFormat's own locale-derived pattern —
// standard JVM convention is an uppercase 'H' for a 24-hour pattern versus
// lowercase 'h' for 12-hour.
class DesktopLocaleFormatting : LocaleFormatting {
    override fun is24HourFormat(): Boolean {
        val pattern = (DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()) as SimpleDateFormat).toPattern()
        return pattern.contains('H')
    }
}

// Desktop actual for HapticAlerter — no standard Linux desktop vibration API
// (and no vibration hardware on a desktop machine), so this is a no-op,
// matching DesktopWidgetRefresher's reasoning above.
class DesktopHapticAlerter : HapticAlerter {
    override fun start(patternId: String) {}
    override fun stop() {}
}

// Desktop actual for ReminderScheduling (Stage 12). Deliberately still a
// no-op: the real firing mechanism is DesktopAlarmClockService, a session-wide
// poller that re-derives what's due by reading the DB directly every tick
// rather than tracking individually-scheduled items — so there's nothing for
// a per-item schedule/cancel call to actually do. (Stage 7's DB write from
// AlarmViewModel/TimerViewModel/ReminderViewModel already persists the data
// these calls would otherwise have needed to mirror into an OS scheduler.)
// Habit-level daily reminders (scheduleReminder/cancelReminder) and Todo due
// alerts (scheduleTodo/cancelTodo) are out of scope for Stage 12 — desktop's
// Habit/Todo UI doesn't expose editing a reminder time yet, so there's
// nothing to schedule even on Android's model.
class DesktopReminderScheduling : ReminderScheduling {
    override fun scheduleReminder(habit: Habit) {}
    override fun cancelReminder(habitId: Long) {}
    override fun scheduleOneShot(reminder: Reminder) {}
    override fun cancelOneShot(reminderId: Long) {}
    override fun scheduleAlarm(alarm: Alarm) {}
    override fun cancelAlarm(alarmId: Long) {}
    override fun scheduleTodo(todo: Todo) {}
    override fun cancelTodo(todoId: Long) {}
    override fun scheduleTimerCompletion(timerId: Long, endTimeMillis: Long, label: String) {}
    override fun cancelTimerCompletion(timerId: Long) {}
}
