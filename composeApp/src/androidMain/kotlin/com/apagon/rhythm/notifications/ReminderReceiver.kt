package com.apagon.rhythm.notifications
import com.apagon.rhythm.core.time.*

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.apagon.rhythm.ui.util.VibrationPatterns
import com.apagon.rhythm.R
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.repository.AlarmRepository
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.ui.reminders.GenericAlertActivity
import com.apagon.rhythm.ui.alarms.AlarmAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
class ReminderReceiver : BroadcastReceiver(), KoinComponent {

    val habitRepository: HabitRepository by inject()
    val alarmRepository: AlarmRepository by inject()
    val todoRepository: com.apagon.rhythm.data.repository.TodoRepository by inject()
    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                ACTION_COMPLETE -> handleComplete(context, intent)
                ACTION_SNOOZE   -> handleSnooze(context, intent)
                ACTION_DISMISS  -> handleDismiss(context, intent)
                else            -> handleAlarm(context, intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleComplete(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
        getVibrator(context).cancel()
        when (intent.getStringExtra(EXTRA_TYPE)) {
            TYPE_ALARM -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId != -1L) sendAlarmStop(context, alarmId)
            }
            TYPE_HABIT -> {
                val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                if (habitId == -1L) return
                sendGenericStop(context, TYPE_HABIT, habitId)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        habitRepository.markComplete(habitId, LocalDate.now().toString())
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            TYPE_REMINDER -> {
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                if (reminderId != -1L) sendGenericStop(context, TYPE_REMINDER, reminderId)
            }
            TYPE_TODO -> {
                val todoId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                if (todoId != -1L) {
                    sendGenericStop(context, TYPE_TODO, todoId)
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            todoRepository.markComplete(todoId)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
        getVibrator(context).cancel()
        val type = intent.getStringExtra(EXTRA_TYPE) ?: return
        if (type == TYPE_ALARM) {
            val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
            if (alarmId == -1L) return
            val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: ""
            val soundUri = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""
            val vibrationPatternId = intent.getStringExtra(EXTRA_VIBRATION_PATTERN_ID) ?: "default"
            val repeatDays = intent.getIntExtra(EXTRA_REPEAT_DAYS, 0)
            val hour = intent.getIntExtra(EXTRA_ALARM_HOUR, 0)
            val minute = intent.getIntExtra(EXTRA_ALARM_MINUTE, 0)
            sendAlarmStop(context, alarmId)
            ReminderScheduler.scheduleSnooze(
                context, TYPE_ALARM, alarmId, label, "",
                soundUri = soundUri, vibrationPatternId = vibrationPatternId,
                repeatDays = repeatDays, hour = hour, minute = minute
            )
            return
        }
        if (type == TYPE_HABIT) {
            val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
            if (habitId == -1L) return
            val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: return
            val reminderTime = intent.getStringExtra(EXTRA_REMINDER_TIME) ?: return
            val soundUri = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""
            val vibrationPatternId = intent.getStringExtra(EXTRA_VIBRATION_PATTERN_ID) ?: "default"
            sendGenericStop(context, TYPE_HABIT, habitId)
            ReminderScheduler.scheduleSnooze(context, TYPE_HABIT, habitId, habitName, reminderTime, soundUri = soundUri, vibrationPatternId = vibrationPatternId)
        } else {
            val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
            if (reminderId == -1L) return
            val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: return
            val note = intent.getStringExtra(EXTRA_REMINDER_NOTE) ?: ""
            val soundUri = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""
            val vibrationPatternId = intent.getStringExtra(EXTRA_VIBRATION_PATTERN_ID) ?: "default"
            sendGenericStop(context, type, reminderId)
            ReminderScheduler.scheduleSnooze(context, type, reminderId, title, note, soundUri, vibrationPatternId)
        }
    }

    private fun handleAlarm(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_HABIT
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (type) {
            TYPE_ALARM    -> handleAlarmNotification(context, intent, nm, isSnooze)
            TYPE_REMINDER, TYPE_EVENT, TYPE_TODO -> handleReminderNotification(context, intent, nm)
            else          -> handleHabitNotification(context, intent, nm, isSnooze)
        }
    }

    private fun handleAlarmNotification(
        context: Context,
        intent: Intent,
        nm: NotificationManager,
        isSnooze: Boolean
    ) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""
        val vibrationPatternId = intent.getStringExtra(EXTRA_VIBRATION_PATTERN_ID) ?: "default"
        val repeatDays = intent.getIntExtra(EXTRA_REPEAT_DAYS, 0)
        val hour = intent.getIntExtra(EXTRA_ALARM_HOUR, 0)
        val minute = intent.getIntExtra(EXTRA_ALARM_MINUTE, 0)
        val notifId = ((alarmId + ReminderScheduler.ALARM_ID_OFFSET) % Int.MAX_VALUE).toInt().coerceAtLeast(1)

        val dismissPi = actionPendingIntent(context, ACTION_COMPLETE, notifId, TYPE_ALARM) {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val snoozePi = actionPendingIntent(context, ACTION_SNOOZE, notifId, TYPE_ALARM) {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_SOUND_URI, soundUri)
            putExtra(EXTRA_VIBRATION_PATTERN_ID, vibrationPatternId)
            putExtra(EXTRA_REPEAT_DAYS, repeatDays)
            putExtra(EXTRA_ALARM_HOUR, hour)
            putExtra(EXTRA_ALARM_MINUTE, minute)
        }

        val alarmChannelId = resolveAlarmChannel(context, nm, soundUri, vibrationPatternId)
        val alarmFullScreenPi = alarmFullScreenIntent(
            context, notifId, alarmId, label, soundUri, repeatDays, hour, minute
        )
        startLoopingVibration(context, VibrationPatterns.patternOf(vibrationPatternId))
        val alarmNotification = NotificationCompat.Builder(context, alarmChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(label.ifBlank { "Alarm" })
            .setContentText("Tap to dismiss or snooze.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(0)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(alarmFullScreenPi)
            .apply { if (canUseFullScreenIntent(context)) setFullScreenIntent(alarmFullScreenPi, true) }
            .addAction(0, "Dismiss", dismissPi)
            .addAction(0, "Snooze 5", snoozePi)
            .build()
            .apply { if (soundUri != "silent") flags = flags or android.app.Notification.FLAG_INSISTENT }
        nm.notify(notifId, alarmNotification)

        if (repeatDays == 0) {
            // One-time alarm: disable it
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    alarmRepository.updateAlarm(
                        Alarm(id = alarmId, label = label, hour = hour, minute = minute,
                              repeatDaysMask = repeatDays, isEnabled = false, soundUri = soundUri)
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (!isSnooze) {
            // Repeating alarm: schedule next occurrence
            ReminderScheduler.scheduleAlarm(
                context,
                Alarm(id = alarmId, label = label, hour = hour, minute = minute,
                      repeatDaysMask = repeatDays, isEnabled = true, soundUri = soundUri,
                      vibrationPatternId = vibrationPatternId)
            )
        }
    }

    private fun handleReminderNotification(context: Context, intent: Intent, nm: NotificationManager) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: return
        val note = intent.getStringExtra(EXTRA_REMINDER_NOTE) ?: ""
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""
        val vibrationPatternId = intent.getStringExtra(EXTRA_VIBRATION_PATTERN_ID) ?: "default"
        val actualType = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_REMINDER
        val notifId = ((reminderId + ReminderScheduler.REMINDER_ID_OFFSET) % Int.MAX_VALUE).toInt().coerceAtLeast(1)

        val channelId = resolveChannel(context, nm, soundUri, vibrationPatternId)
        val fullScreenPi = genericFullScreenIntent(context, notifId, reminderId, title, note, soundUri, actualType, vibrationPatternId)
        val completePi = actionPendingIntent(context, ACTION_COMPLETE, notifId, actualType) {
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val snoozePi = actionPendingIntent(context, ACTION_SNOOZE, notifId, actualType) {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_REMINDER_NOTE, note)
            putExtra(EXTRA_SOUND_URI, soundUri)
        }
        val dismissPiReminder = actionPendingIntent(context, ACTION_DISMISS, notifId + 5000, actualType) {
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        startLoopingVibration(context, VibrationPatterns.patternOf(vibrationPatternId))
        nm.notify(notifId, NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(note.ifBlank { "Time for your reminder!" })
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenPi)
            .setDeleteIntent(dismissPiReminder)
            .addAction(0, "Done", completePi)
            .addAction(0, "Snooze 5", snoozePi)
            .build())
    }

    private fun handleHabitNotification(
        context: Context,
        intent: Intent,
        nm: NotificationManager,
        isSnooze: Boolean
    ) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId == -1L) return
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: return
        val reminderTime = intent.getStringExtra(EXTRA_REMINDER_TIME) ?: return
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""
        val vibrationPatternId = intent.getStringExtra(EXTRA_VIBRATION_PATTERN_ID) ?: "default"
        val notifId = (habitId % Int.MAX_VALUE).toInt().coerceAtLeast(1)

        val fullScreenPi = genericFullScreenIntent(context, notifId, habitId, habitName, reminderTime, soundUri, TYPE_HABIT, vibrationPatternId)
        val completePi = actionPendingIntent(context, ACTION_COMPLETE, notifId, TYPE_HABIT) {
            putExtra(EXTRA_HABIT_ID, habitId)
        }
        val snoozePi = actionPendingIntent(context, ACTION_SNOOZE, notifId, TYPE_HABIT) {
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_REMINDER_TIME, reminderTime)
        }
        val dismissPiHabit = actionPendingIntent(context, ACTION_DISMISS, notifId + 5000, TYPE_HABIT) {
            putExtra(EXTRA_HABIT_ID, habitId)
        }

        startLoopingVibration(context, VibrationPatterns.patternOf(vibrationPatternId))
        nm.notify(notifId, NotificationCompat.Builder(context, resolveChannel(context, nm, soundUri, vibrationPatternId))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(habitName)
            .setContentText("Time for your habit!")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenPi)
            .setDeleteIntent(dismissPiHabit)
            .addAction(0, "Done", completePi)
            .addAction(0, "Snooze 5", snoozePi)
            .build())

        if (!isSnooze) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val habit = habitRepository.getHabitById(habitId).first()
                    if (habit != null) {
                        ReminderScheduler.scheduleReminder(context, habit)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun ensureDynamicChannel(nm: NotificationManager, soundUri: String, usage: Int, vibPattern: LongArray): String {
        val channelId = "dyn3_${soundUri.hashCode()}_${vibPattern.contentHashCode()}"
        if (nm.getNotificationChannel(channelId) == null) {
            val uri = runCatching { Uri.parse(soundUri) }.getOrNull()
                ?: return CHANNEL_ID_REMINDER_ALERT
            val audioAttr = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(uri, audioAttr)
                    enableVibration(true)
                    vibrationPattern = vibPattern
                    setShowBadge(true)
                }
            )
        }
        return channelId
    }

    private fun ensureSilentDynChannel(nm: NotificationManager, vibPattern: LongArray): String {
        val channelId = "silent_dyn_${vibPattern.contentHashCode()}"
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Silent Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    enableVibration(true)
                    vibrationPattern = vibPattern
                    setShowBadge(true)
                }
            )
        }
        return channelId
    }

    private fun ensureDefaultSoundDynChannel(nm: NotificationManager, vibPattern: LongArray): String {
        val channelId = "default_dyn_${vibPattern.contentHashCode()}"
        if (nm.getNotificationChannel(channelId) == null) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(uri, audioAttr)
                    enableVibration(true)
                    vibrationPattern = vibPattern
                    setShowBadge(true)
                }
            )
        }
        return channelId
    }

    private fun resolveChannel(context: Context, nm: NotificationManager, soundUri: String, patternId: String = "default"): String {
        val vibPattern = VibrationPatterns.patternOf(patternId)
        if (soundUri == "silent") {
            return if (patternId == "default") CHANNEL_ID_SILENT_HIGH else ensureSilentDynChannel(nm, vibPattern)
        }
        if (soundUri.isEmpty()) {
            return if (patternId == "default") CHANNEL_ID_REMINDER_ALERT else ensureDefaultSoundDynChannel(nm, vibPattern)
        }
        return ensureDynamicChannel(nm, soundUri, AudioAttributes.USAGE_ALARM, vibPattern)
    }

    private fun resolveAlarmChannel(context: Context, nm: NotificationManager, soundUri: String, patternId: String = "default"): String {
        val vibPattern = VibrationPatterns.patternOf(patternId)
        if (soundUri == "silent") {
            return if (patternId == "default") CHANNEL_ID_SILENT_HIGH else ensureSilentDynChannel(nm, vibPattern)
        }
        if (soundUri.isEmpty()) {
            return if (patternId == "default") CHANNEL_ID_REMINDER_ALERT else ensureDefaultSoundDynChannel(nm, vibPattern)
        }
        return ensureDynamicChannel(nm, soundUri, AudioAttributes.USAGE_ALARM, vibPattern)
    }

    private fun getVibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private fun startLoopingVibration(context: Context, vibPattern: LongArray) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(vibPattern, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build()
                vibrator.vibrate(effect, attrs)
            } else {
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, audioAttrs)
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibPattern, 0)
        }
    }

    private fun genericFullScreenIntent(
        context: Context, notifId: Int, id: Long, title: String, note: String, soundUri: String, type: String, vibrationPatternId: String = "default"
    ): PendingIntent {
        val intent = Intent(context, GenericAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(EXTRA_NOTIF_ID, notifId)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_VIBRATION_PATTERN_ID, vibrationPatternId)
            if (type == TYPE_HABIT) {
                putExtra(EXTRA_HABIT_ID, id)
                putExtra(EXTRA_HABIT_NAME, title)
                putExtra(EXTRA_REMINDER_TIME, note)
                putExtra(EXTRA_SOUND_URI, soundUri)
            } else {
                putExtra(EXTRA_REMINDER_ID, id)
                putExtra(EXTRA_REMINDER_TITLE, title)
                putExtra(EXTRA_REMINDER_NOTE, note)
                putExtra(EXTRA_SOUND_URI, soundUri)
            }
        }
        return PendingIntent.getActivity(context, notifId + 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun alarmFullScreenIntent(
        context: Context, notifId: Int, alarmId: Long, label: String, soundUri: String, repeatDays: Int, hour: Int, minute: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NOTIF_ID, notifId)
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_SOUND_URI, soundUri)
            putExtra(EXTRA_REPEAT_DAYS, repeatDays)
            putExtra(EXTRA_ALARM_HOUR, hour)
            putExtra(EXTRA_ALARM_MINUTE, minute)
        }
        return PendingIntent.getActivity(context, notifId + 2000, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionPendingIntent(
        context: Context, action: String, notifId: Int, type: String, extras: Intent.() -> Unit = {}
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_NOTIF_ID, notifId)
            putExtra(EXTRA_TYPE, type)
            extras()
        }
        return PendingIntent.getBroadcast(context, notifId + 3000 + action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent()
        } else {
            true
        }
    }

    private fun sendAlarmStop(context: Context, alarmId: Long) {
        val actionId = ((alarmId + ReminderScheduler.ALARM_ID_OFFSET) % Int.MAX_VALUE).toInt()
        context.sendBroadcast(
            Intent("${AlarmAlertActivity.ACTION_ALARM_STOP}_$alarmId").apply {
                setPackage(context.packageName)
            }
        )
    }

    private fun sendGenericStop(context: Context, type: String, id: Long) {
        val actionId = if (type == TYPE_HABIT) {
            (id % Int.MAX_VALUE).toInt()
        } else {
            ((id + ReminderScheduler.REMINDER_ID_OFFSET) % Int.MAX_VALUE).toInt()
        }
        context.sendBroadcast(
            Intent("${GenericAlertActivity.ACTION_STOP}_$actionId").apply {
                setPackage(context.packageName)
            }
        )
    }

    private fun handleDismiss(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
        getVibrator(context).cancel()
        when (intent.getStringExtra(EXTRA_TYPE)) {
            TYPE_HABIT -> {
                val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                if (habitId != -1L) sendGenericStop(context, TYPE_HABIT, habitId)
            }
            TYPE_REMINDER -> {
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                if (reminderId != -1L) sendGenericStop(context, TYPE_REMINDER, reminderId)
            }
            TYPE_ALARM -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId != -1L) sendAlarmStop(context, alarmId)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "reminders_v4"
        const val CHANNEL_ID_SILENT = "reminders_silent"
        const val CHANNEL_ID_ALARM = "alarms_v2"
        const val CHANNEL_ID_SILENT_HIGH = "silent_high_v2"
        const val CHANNEL_ID_REMINDER_ALERT = "reminder_alerts_v4"

        const val ACTION_COMPLETE = "com.apagon.rhythm.ACTION_COMPLETE"
        const val ACTION_SNOOZE   = "com.apagon.rhythm.ACTION_SNOOZE"
        const val ACTION_DISMISS  = "com.apagon.rhythm.ACTION_DISMISS"

        const val EXTRA_NOTIF_ID = "notif_id"
        const val EXTRA_TYPE     = "type"
        const val EXTRA_IS_SNOOZE = "is_snooze"

        const val TYPE_HABIT    = "habit"
        const val TYPE_REMINDER = "reminder"
        const val TYPE_ALARM    = "alarm"
        const val TYPE_EVENT    = "event"
        const val TYPE_TODO     = "todo"

        const val EXTRA_HABIT_ID   = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_REMINDER_TIME = "reminder_time"

        const val EXTRA_REMINDER_ID    = "reminder_id"
        const val EXTRA_REMINDER_TITLE = "reminder_title"
        const val EXTRA_REMINDER_NOTE  = "reminder_note"

        const val EXTRA_ALARM_ID     = "alarmId"
        const val EXTRA_ALARM_LABEL  = "label"
        const val EXTRA_REPEAT_DAYS  = "repeatDays"
        const val EXTRA_ALARM_HOUR   = "hour"
        const val EXTRA_ALARM_MINUTE = "minute"

        const val EXTRA_SOUND_URI = "sound_uri"
        const val EXTRA_VIBRATION_PATTERN_ID = "vibration_pattern_id"
    }
}
