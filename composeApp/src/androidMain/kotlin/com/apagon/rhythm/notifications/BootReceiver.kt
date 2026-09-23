package com.apagon.rhythm.notifications
import com.apagon.rhythm.core.time.*

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apagon.rhythm.data.repository.AlarmRepository
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.ReminderRepository
import com.apagon.rhythm.ui.util.REMINDER_INPUT_FMT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
class BootReceiver : BroadcastReceiver(), KoinComponent {

    val habitRepository: HabitRepository by inject()
    val reminderRepository: ReminderRepository by inject()
    val alarmRepository: AlarmRepository by inject()
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                habitRepository.getAllActiveHabits().first().forEach { habit ->
                    ReminderScheduler.scheduleReminder(context, habit)
                }
                val now = LocalDateTime.now()
                reminderRepository.getAllActiveReminders().first()
                    .filter {
                        try { LocalDateTime.parse(it.dateTime, REMINDER_INPUT_FMT).isAfter(now) }
                        catch (e: Exception) { false }
                    }
                    .forEach { reminder ->
                        ReminderScheduler.scheduleOneShot(context, reminder)
                    }
                alarmRepository.getEnabledAlarms().forEach { alarm ->
                    ReminderScheduler.scheduleAlarm(context, alarm)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
