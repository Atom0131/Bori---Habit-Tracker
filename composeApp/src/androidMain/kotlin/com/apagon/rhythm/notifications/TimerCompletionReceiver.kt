package com.apagon.rhythm.notifications

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
import com.apagon.rhythm.data.repository.TimerRepository
import com.apagon.rhythm.ui.util.VibrationPatterns
import com.apagon.rhythm.ui.alarms.TimerAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext
class TimerCompletionReceiver : BroadcastReceiver(), KoinComponent {

    val repository: TimerRepository by inject()
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
            if (timerId == -1L) return

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (intent.action == ACTION_DISMISS) {
                handleDismiss(context, nm, timerId)
                return
            }

            if (intent.action == ACTION_RESTART) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try { handleRestart(context, nm, timerId) } finally { pendingResult.finish() }
                }
                return
            }

            if (intent.action == ACTION_POMO_ADVANCE) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try { handlePomoAdvance(context, nm, timerId) } finally { pendingResult.finish() }
                }
                return
            }

            if (intent.action == ACTION_ADD_5_MIN) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try { handleAdd5Min(context, nm, timerId) } finally { pendingResult.finish() }
                }
                return
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    handleCompletion(context, nm, timerId)
                } finally {
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleDismiss(context: Context, nm: NotificationManager, timerId: Long) {
        nm.cancel(NOTIF_ID_BASE + timerId.toInt())
        getVibrator(context).cancel()
        context.sendBroadcast(
            Intent("${TimerAlertActivity.ACTION_TIMER_STOP}_$timerId").apply {
                setPackage(context.packageName)
            }
        )
    }

    private suspend fun handleRestart(context: Context, nm: NotificationManager, timerId: Long) {
        val timer = repository.getTimerById(timerId) ?: return
        val endTime = System.currentTimeMillis() + timer.durationSeconds * 1000L
        repository.updateTimer(timer.copy(remainingSeconds = timer.durationSeconds, endTimeMillis = endTime))

        val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val pi = android.app.PendingIntent.getBroadcast(
            context, timerId.toInt(),
            Intent(context, TimerCompletionReceiver::class.java).apply { putExtra(EXTRA_TIMER_ID, timerId) },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, endTime, pi)
        withContext(Dispatchers.Main) {
            context.startService(TimerForegroundService.startIntent(context, endTime, timer.label))
        }

        nm.cancel(NOTIF_ID_BASE + timerId.toInt())
        getVibrator(context).cancel()
        context.sendBroadcast(
            Intent("${TimerAlertActivity.ACTION_TIMER_STOP}_$timerId").apply {
                setPackage(context.packageName)
            }
        )
    }

    private suspend fun handleAdd5Min(context: Context, nm: NotificationManager, timerId: Long) {
        val timer = repository.getTimerById(timerId) ?: return
        val endTime = System.currentTimeMillis() + 5 * 60 * 1000L
        repository.updateTimer(timer.copy(remainingSeconds = 300, endTimeMillis = endTime))
        val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val pi = android.app.PendingIntent.getBroadcast(
            context, timerId.toInt(),
            Intent(context, TimerCompletionReceiver::class.java).apply { putExtra(EXTRA_TIMER_ID, timerId) },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, endTime, pi)
        withContext(Dispatchers.Main) {
            context.startService(TimerForegroundService.startIntent(context, endTime, timer.label))
        }
        nm.cancel(NOTIF_ID_BASE + timerId.toInt())
        getVibrator(context).cancel()
        context.sendBroadcast(
            Intent("${TimerAlertActivity.ACTION_TIMER_STOP}_$timerId").apply {
                setPackage(context.packageName)
            }
        )
    }

    private suspend fun handleCompletion(context: Context, nm: NotificationManager, timerId: Long) {
        val timer = repository.getTimerById(timerId) ?: return

        if (timer.isPomo) {
            handlePomoPhaseComplete(context, nm, timerId, timer)
            return
        }

        repository.updateTimer(timer.copy(remainingSeconds = 0, endTimeMillis = 0))

        val notifId = NOTIF_ID_BASE + timerId.toInt()
        val title = if (timer.label.isNotBlank()) timer.label else "Timer done"

        val nm2 = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val vibPattern = VibrationPatterns.patternOf(timer.vibrationPatternId)
        val channelId = when {
            timer.soundUri == "silent" && timer.vibrationPatternId == "default" -> ReminderReceiver.CHANNEL_ID_SILENT_HIGH
            timer.soundUri == "silent"   -> ensureSilentDynChannel(nm2, vibPattern)
            timer.soundUri.isEmpty() && timer.vibrationPatternId == "default"   -> ReminderReceiver.CHANNEL_ID_REMINDER_ALERT
            timer.soundUri.isEmpty()     -> ensureDefaultSoundDynChannel(nm2, vibPattern)
            else                         -> ensureDynamicChannel(nm2, timer.soundUri, AudioAttributes.USAGE_ALARM, vibPattern)
        }
        val uri: Uri? = when {
            timer.soundUri == "silent" -> null
            timer.soundUri.isEmpty() -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            else -> Uri.parse(timer.soundUri)
        }
        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val fullScreenIntent = Intent(context, TimerAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(TimerAlertActivity.EXTRA_TIMER_ID, timerId)
            putExtra(TimerAlertActivity.EXTRA_TIMER_LABEL, timer.label)
            putExtra(TimerAlertActivity.EXTRA_TIMER_DURATION, timer.durationSeconds)
            putExtra(TimerAlertActivity.EXTRA_SOUND_URI, timer.soundUri)
            putExtra(ReminderReceiver.EXTRA_VIBRATION_PATTERN_ID, timer.vibrationPatternId)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, notifId + 400_000, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, TimerCompletionReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_TIMER_ID, timerId)
        }
        val dismissPi = PendingIntent.getBroadcast(
            context, notifId + 500_000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val restartIntent = Intent(context, TimerCompletionReceiver::class.java).apply {
            action = ACTION_RESTART
            putExtra(EXTRA_TIMER_ID, timerId)
        }
        val restartPi = PendingIntent.getBroadcast(
            context, notifId + 600_000, restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val add5MinIntent = Intent(context, TimerCompletionReceiver::class.java).apply {
            action = ACTION_ADD_5_MIN
            putExtra(EXTRA_TIMER_ID, timerId)
        }
        val add5MinPi = PendingIntent.getBroadcast(
            context, notifId + 700_000, add5MinIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        startLoopingVibration(context, vibPattern)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("${formatShortDuration(timer.durationSeconds)} timer is done!")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(0)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenPi)
            .apply { if (canUseFullScreenIntent(context)) setFullScreenIntent(fullScreenPi, true) }
            .addAction(0, "Done", dismissPi)
            .addAction(0, "+5 Min", add5MinPi)
            .addAction(0, "Restart", restartPi)
            .build().apply {
                if (timer.soundUri != "silent") {
                    flags = flags or android.app.Notification.FLAG_INSISTENT
                }
            }
        nm.notify(notifId, notification)

        // Stop foreground service if no more timers are running
        val anyRunning = repository.getAllTimers().first().any { it.endTimeMillis > 0 }
        if (!anyRunning) {
            context.startService(TimerForegroundService.stopIntent(context))
        }
    }

    private suspend fun handlePomoPhaseComplete(
        context: Context,
        nm: NotificationManager,
        timerId: Long,
        timer: com.apagon.rhythm.data.model.Timer
    ) {
        val (nextPhase, nextSession, isRoundDone) = nextPomoState(timer)

        if (isRoundDone) {
            // Reset to start of round and show completion alert
            repository.updateTimer(
                timer.copy(
                    durationSeconds = timer.pomoWorkSecs,
                    remainingSeconds = timer.pomoWorkSecs,
                    endTimeMillis = 0,
                    pomoCurrentSession = 1,
                    pomoPhase = "WORK"
                )
            )

            val notifId = NOTIF_ID_BASE + timerId.toInt()
            val title = if (timer.label.isNotBlank()) "${timer.label} complete!" else "Pomodoro complete!"
            val nm2 = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val vibPattern = VibrationPatterns.patternOf(timer.vibrationPatternId)
            val channelId = when {
                timer.soundUri == "silent" && timer.vibrationPatternId == "default" -> ReminderReceiver.CHANNEL_ID_SILENT_HIGH
                timer.soundUri == "silent"   -> ensureSilentDynChannel(nm2, vibPattern)
                timer.soundUri.isEmpty() && timer.vibrationPatternId == "default"   -> ReminderReceiver.CHANNEL_ID_REMINDER_ALERT
                timer.soundUri.isEmpty()     -> ensureDefaultSoundDynChannel(nm2, vibPattern)
                else                         -> ensureDynamicChannel(nm2, timer.soundUri, AudioAttributes.USAGE_ALARM, vibPattern)
            }
            val uri: Uri? = when {
                timer.soundUri == "silent" -> null
                timer.soundUri.isEmpty() -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                else -> Uri.parse(timer.soundUri)
            }

            val fullScreenIntent = Intent(context, TimerAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(TimerAlertActivity.EXTRA_TIMER_ID, timerId)
                putExtra(TimerAlertActivity.EXTRA_TIMER_LABEL, timer.label)
                putExtra(TimerAlertActivity.EXTRA_TIMER_DURATION, timer.pomoWorkSecs)
                putExtra(TimerAlertActivity.EXTRA_SOUND_URI, timer.soundUri)
                putExtra(ReminderReceiver.EXTRA_VIBRATION_PATTERN_ID, timer.vibrationPatternId)
            }
            val fullScreenPi = PendingIntent.getActivity(
                context, notifId + 400_000, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val dismissIntent = Intent(context, TimerCompletionReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_TIMER_ID, timerId)
            }
            val dismissPi = PendingIntent.getBroadcast(
                context, notifId + 500_000, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val restartIntent = Intent(context, TimerCompletionReceiver::class.java).apply {
                action = ACTION_RESTART
                putExtra(EXTRA_TIMER_ID, timerId)
            }
            val restartPi = PendingIntent.getBroadcast(
                context, notifId + 600_000, restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val add5MinPomoIntent = Intent(context, TimerCompletionReceiver::class.java).apply {
                action = ACTION_ADD_5_MIN
                putExtra(EXTRA_TIMER_ID, timerId)
            }
            val add5MinPomoPi = PendingIntent.getBroadcast(
                context, notifId + 700_000, add5MinPomoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startLoopingVibration(context, vibPattern)
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText("All ${timer.pomoSessionsPerRound} sessions done!")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(0)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(fullScreenPi)
                .apply { if (canUseFullScreenIntent(context)) setFullScreenIntent(fullScreenPi, true) }
                .addAction(0, "Done", dismissPi)
                .addAction(0, "+5 Min", add5MinPomoPi)
                .addAction(0, "Restart", restartPi)
                .build().apply {
                    if (timer.soundUri != "silent") {
                        flags = flags or android.app.Notification.FLAG_INSISTENT
                    }
                }
            nm.notify(notifId, notification)
            val anyRunning = repository.getAllTimers().first().any { it.endTimeMillis > 0 }
            if (!anyRunning) {
                context.startService(TimerForegroundService.stopIntent(context))
            }
        } else {
            // Pause at end of current phase and wait for user to tap Done
            repository.updateTimer(timer.copy(remainingSeconds = 0, endTimeMillis = 0))

            val notifId = NOTIF_ID_BASE + timerId.toInt()
            val nm2 = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val vibPattern = VibrationPatterns.patternOf(timer.vibrationPatternId)
            val channelId = when {
                timer.soundUri == "silent" && timer.vibrationPatternId == "default" -> ReminderReceiver.CHANNEL_ID_SILENT_HIGH
                timer.soundUri == "silent"   -> ensureSilentDynChannel(nm2, vibPattern)
                timer.soundUri.isEmpty() && timer.vibrationPatternId == "default"   -> ReminderReceiver.CHANNEL_ID_REMINDER_ALERT
                timer.soundUri.isEmpty()     -> ensureDefaultSoundDynChannel(nm2, vibPattern)
                else                         -> ensureDynamicChannel(nm2, timer.soundUri, AudioAttributes.USAGE_ALARM, vibPattern)
            }
            val (title, body) = when (timer.pomoPhase) {
                "WORK" -> "Work session done!" to "Tap Done to start break"
                else   -> "Break done!" to "Tap Done to start next work session"
            }
            val uri: Uri? = when {
                timer.soundUri == "silent" -> null
                timer.soundUri.isEmpty() -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                else -> Uri.parse(timer.soundUri)
            }

            val phaseDuration = when (timer.pomoPhase) {
                "WORK"        -> timer.pomoWorkSecs
                "SHORT_BREAK" -> timer.pomoShortBreakSecs
                else          -> timer.pomoLongBreakSecs
            }
            val fullScreenIntent = Intent(context, TimerAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(TimerAlertActivity.EXTRA_TIMER_ID, timerId)
                putExtra(TimerAlertActivity.EXTRA_TIMER_LABEL, if (timer.label.isNotBlank()) timer.label else title)
                putExtra(TimerAlertActivity.EXTRA_TIMER_DURATION, phaseDuration)
                putExtra(TimerAlertActivity.EXTRA_SOUND_URI, timer.soundUri)
                putExtra(ReminderReceiver.EXTRA_VIBRATION_PATTERN_ID, timer.vibrationPatternId)
                putExtra(TimerAlertActivity.EXTRA_POMO_ADVANCE, true)
                putExtra("pomo_phase", timer.pomoPhase)
            }
            val fullScreenPi = PendingIntent.getActivity(
                context, notifId + 400_000, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val pomoAdvanceIntent = Intent(context, TimerCompletionReceiver::class.java).apply {
                action = ACTION_POMO_ADVANCE
                putExtra(EXTRA_TIMER_ID, timerId)
            }
            val pomoAdvancePi = PendingIntent.getBroadcast(
                context, notifId + 500_000, pomoAdvanceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val restartIntent = Intent(context, TimerCompletionReceiver::class.java).apply {
                action = ACTION_RESTART
                putExtra(EXTRA_TIMER_ID, timerId)
            }
            val restartPi = PendingIntent.getBroadcast(
                context, notifId + 600_000, restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            startLoopingVibration(context, vibPattern)
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(0)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(fullScreenPi)
                .apply { if (canUseFullScreenIntent(context)) setFullScreenIntent(fullScreenPi, true) }
                .addAction(0, "Done", pomoAdvancePi)
                .addAction(0, "Restart", restartPi)
                .build().apply {
                    if (timer.soundUri != "silent") {
                        flags = flags or android.app.Notification.FLAG_INSISTENT
                    }
                }
            nm.notify(notifId, notification)

            val anyRunning = repository.getAllTimers().first().any { it.endTimeMillis > 0 }
            if (!anyRunning) {
                context.startService(TimerForegroundService.stopIntent(context))
            }
        }
    }

    private suspend fun handlePomoAdvance(context: Context, nm: NotificationManager, timerId: Long) {
        val timer = repository.getTimerById(timerId) ?: return
        val (nextPhase, nextSession, _) = nextPomoState(timer)
        val nextDuration = when (nextPhase) {
            "WORK"        -> timer.pomoWorkSecs
            "SHORT_BREAK" -> timer.pomoShortBreakSecs
            else          -> timer.pomoLongBreakSecs
        }
        val endTime = System.currentTimeMillis() + nextDuration * 1000L
        repository.updateTimer(
            timer.copy(
                durationSeconds    = nextDuration,
                remainingSeconds   = nextDuration,
                endTimeMillis      = endTime,
                pomoCurrentSession = nextSession,
                pomoPhase          = nextPhase
            )
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val pi = android.app.PendingIntent.getBroadcast(
            context, timerId.toInt(),
            Intent(context, TimerCompletionReceiver::class.java).apply { putExtra(EXTRA_TIMER_ID, timerId) },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, endTime, pi)
        withContext(Dispatchers.Main) {
            context.startService(TimerForegroundService.startIntent(context, endTime, timer.label))
        }
        nm.cancel(NOTIF_ID_BASE + timerId.toInt())
        context.sendBroadcast(
            Intent("${TimerAlertActivity.ACTION_TIMER_STOP}_$timerId").apply {
                setPackage(context.packageName)
            }
        )
    }

    private fun nextPomoState(timer: com.apagon.rhythm.data.model.Timer): Triple<String, Int, Boolean> {
        return when (timer.pomoPhase) {
            "WORK" -> if (timer.pomoCurrentSession >= timer.pomoSessionsPerRound) {
                Triple("LONG_BREAK", timer.pomoCurrentSession, false)
            } else {
                Triple("SHORT_BREAK", timer.pomoCurrentSession, false)
            }
            "SHORT_BREAK" -> Triple("WORK", timer.pomoCurrentSession + 1, false)
            "LONG_BREAK"  -> Triple("WORK", 1, true)
            else          -> Triple("WORK", 1, true)
        }
    }

    private fun ensureDynamicChannel(nm: NotificationManager, soundUri: String, usage: Int, vibPattern: LongArray): String {
        val channelId = "dyn3_${soundUri.hashCode()}_${vibPattern.contentHashCode()}"
        if (nm.getNotificationChannel(channelId) == null) {
            val uri = runCatching { Uri.parse(soundUri) }.getOrNull()
                ?: return ReminderReceiver.CHANNEL_ID_REMINDER_ALERT
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

    private fun formatShortDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0           -> "${h}h"
            m > 0           -> "${m}m"
            else            -> "${seconds}s"
        }
    }

    private fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent()
        } else {
            true
        }
    }

    companion object {
        const val EXTRA_TIMER_ID = "timer_id"
        const val ACTION_DISMISS      = "com.apagon.rhythm.ACTION_TIMER_DISMISS"
        const val ACTION_RESTART      = "com.apagon.rhythm.ACTION_TIMER_RESTART"
        const val ACTION_POMO_ADVANCE = "com.apagon.rhythm.ACTION_POMO_ADVANCE"
        const val ACTION_ADD_5_MIN    = "com.apagon.rhythm.ACTION_TIMER_ADD_5_MIN"
        private const val NOTIF_ID_BASE = 900_000
    }
}
