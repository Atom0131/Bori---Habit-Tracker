package com.apagon.rhythm.platform

import com.apagon.rhythm.core.time.now
import com.apagon.rhythm.core.time.parse
import com.apagon.rhythm.data.db.AlarmDao
import com.apagon.rhythm.data.db.ReminderDao
import com.apagon.rhythm.data.db.TimerDao
import com.apagon.rhythm.ui.util.REMINDER_INPUT_FMT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber

// Stage 12's in-process JVM scheduler — replaces AlarmManager/PendingIntent/
// BroadcastReceiver wholesale, per DesktopNoOpReminderScheduling's own "replace
// this whole class wholesale" note. No per-item registration to mirror:
// AlarmManager tracks individually-scheduled items, but this just re-derives
// what's due by polling the DB directly every tick, which is simpler and
// correct for a single always-running desktop process. No boot persistence —
// same accepted limitation the roadmap names for the whole subsystem — this
// only fires while the app is actually running.
class DesktopAlarmClockService(
    private val alarmDao: AlarmDao,
    private val reminderDao: ReminderDao,
    private val timerDao: TimerDao,
    private val alertCenter: AlertCenter
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // In-memory de-dup only — an alarm matching hour:minute would otherwise
    // re-fire on every poll tick within that same minute; a reminder past its
    // due time would re-fire every tick forever. Session-scoped by design:
    // restarting the app is the only way to "miss" a re-fire suppression,
    // which matches the no-boot-persistence limitation above.
    private val firedAlarmMinutes = mutableSetOf<String>()
    private val firedReminderIds = mutableSetOf<Long>()

    fun start() {
        scope.launch {
            while (true) {
                runCatching { tick() }
                delay(10_000L)
            }
        }
    }

    private suspend fun tick() {
        val now = LocalDateTime.now()
        checkAlarms(now)
        checkReminders(now)
        checkTimers()
    }

    private suspend fun checkAlarms(now: LocalDateTime) {
        val bit = 1 shl (now.dayOfWeek.isoDayNumber - 1)
        alarmDao.getEnabledAlarms().forEach { alarm ->
            val matchesTime = alarm.hour == now.hour && alarm.minute == now.minute
            val matchesDay = alarm.repeatDaysMask == 0 || (alarm.repeatDaysMask and bit) != 0
            if (!matchesTime || !matchesDay) return@forEach
            val key = "${alarm.id}@${now.date}T${now.hour}:${now.minute}"
            if (!firedAlarmMinutes.add(key)) return@forEach
            alertCenter.raise(
                FiredAlert(
                    kind = FiredAlertKind.ALARM,
                    sourceId = alarm.id,
                    title = alarm.label.ifBlank { "Alarm" },
                    subtitle = "%02d:%02d".format(alarm.hour, alarm.minute)
                )
            )
            // repeatDaysMask == 0 means one-time: auto-disabled after firing (Alarm.kt's own doc).
            if (alarm.repeatDaysMask == 0) {
                alarmDao.update(alarm.copy(isEnabled = false))
            }
        }
    }

    private suspend fun checkReminders(now: LocalDateTime) {
        reminderDao.getAllActiveReminders().first().forEach { reminder ->
            if (reminder.isCompleted) return@forEach
            val due = runCatching { LocalDateTime.parse(reminder.dateTime, REMINDER_INPUT_FMT) }.getOrNull() ?: return@forEach
            if (due > now) return@forEach
            if (!firedReminderIds.add(reminder.id)) return@forEach
            alertCenter.raise(
                FiredAlert(
                    kind = FiredAlertKind.REMINDER,
                    sourceId = reminder.id,
                    title = reminder.title,
                    subtitle = reminder.note
                )
            )
        }
    }

    private suspend fun checkTimers() {
        val nowMs = com.apagon.rhythm.core.time.System.currentTimeMillis()
        timerDao.getAllTimers().first().forEach { timer ->
            if (timer.endTimeMillis <= 0 || timer.endTimeMillis > nowMs) return@forEach
            alertCenter.raise(
                FiredAlert(
                    kind = FiredAlertKind.TIMER,
                    sourceId = timer.id,
                    title = timer.label.ifBlank { "Timer" },
                    subtitle = "Time's up",
                    isPomo = timer.isPomo
                )
            )
            // Stop the countdown; Pomodoro phase advance is user-triggered from
            // the alert window (matches Android — see TimerRepository.advancePomoPhase).
            timerDao.update(timer.copy(endTimeMillis = 0, remainingSeconds = 0))
        }
    }
}
