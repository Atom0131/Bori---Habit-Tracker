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

// Temporary desktop actual for ReminderScheduling — Stage 7 (Todos) needs
// *something* wired here since TodoViewModel takes it as a hard constructor
// dependency, but the real desktop scheduler (an in-process JVM timer plus
// an OS-notification/window firing path — see ref_notes/) is Stage 12's
// job, the largest single piece of net-new design in the full-port roadmap.
// Until then, todos can be added/completed/deleted normally; only the
// due-time alert itself doesn't fire. Replace this whole class wholesale
// in Stage 12 rather than growing it method-by-method.
class DesktopNoOpReminderScheduling : ReminderScheduling {
    override fun scheduleReminder(habit: Habit) {}
    override fun cancelReminder(habitId: Long) {}
    override fun scheduleOneShot(reminder: Reminder) {}
    override fun cancelOneShot(reminderId: Long) {}
    override fun scheduleAlarm(alarm: Alarm) {}
    override fun cancelAlarm(alarmId: Long) {}
    override fun scheduleTodo(todo: Todo) {}
    override fun cancelTodo(todoId: Long) {}
}
