package com.apagon.rhythm.notifications

import org.koin.android.ext.android.inject

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.apagon.rhythm.R
import com.apagon.rhythm.HabitApp
import com.apagon.rhythm.MainActivity
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.data.repository.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
class TimerForegroundService : Service() {

    val repository: TimerRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_CANCEL_TIMER -> {
                val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
                if (timerId != -1L) {
                    serviceScope.launch(Dispatchers.IO) {
                        val timer = repository.getTimerById(timerId) ?: return@launch
                        cancelAlarm(timerId)
                        repository.updateTimer(timer.copy(remainingSeconds = timer.durationSeconds, endTimeMillis = 0))
                        checkAndStop()
                    }
                }
            }
            ACTION_PAUSE_TIMER -> {
                val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
                if (timerId != -1L) {
                    serviceScope.launch(Dispatchers.IO) {
                        val timer = repository.getTimerById(timerId) ?: return@launch
                        val remaining = maxOf(0, ((timer.endTimeMillis - System.currentTimeMillis()) / 1000).toInt())
                        cancelAlarm(timerId)
                        repository.updateTimer(timer.copy(remainingSeconds = remaining, endTimeMillis = 0))
                        checkAndStop()
                    }
                }
            }
            ACTION_ADD_TIME -> {
                val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
                if (timerId != -1L) {
                    serviceScope.launch(Dispatchers.IO) {
                        val timer = repository.getTimerById(timerId) ?: return@launch
                        if (timer.endTimeMillis <= 0) return@launch
                        val newEnd = timer.endTimeMillis + 300_000L
                        cancelAlarm(timerId)
                        scheduleAlarm(timerId, newEnd)
                        repository.updateTimer(timer.copy(endTimeMillis = newEnd))
                    }
                }
            }
            else -> {
                val hintEndTime = intent?.getLongExtra(EXTRA_END_TIME_MILLIS, 0L) ?: 0L
                val hintLabel   = intent?.getStringExtra(EXTRA_TIMER_LABEL) ?: ""
                startForeground(NOTIF_ID, buildSeedNotification(hintEndTime, hintLabel))
                serviceScope.launch {
                    val timers = repository.getAllTimers().first()
                    val running = timers.filter { it.endTimeMillis > 0 }
                    if (running.isNotEmpty()) {
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIF_ID, buildNotification(running))
                    }
                    startTickLoop()
                }
            }
        }
        return START_STICKY
    }

    private suspend fun checkAndStop() {
        val anyRunning = repository.getAllTimers().first().any { it.endTimeMillis > 0 }
        if (!anyRunning) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun alarmPendingIntent(timerId: Long) = PendingIntent.getBroadcast(
        this, timerId.toInt(),
        Intent(this, TimerCompletionReceiver::class.java).apply {
            putExtra(TimerCompletionReceiver.EXTRA_TIMER_ID, timerId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun cancelAlarm(timerId: Long) {
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(alarmPendingIntent(timerId))
    }

    private fun scheduleAlarm(timerId: Long, endTimeMillis: Long) {
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, alarmPendingIntent(timerId))
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = serviceScope.launch {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // No setShortCriticalText — Samsung's Now Bar reads setUsesChronometer + setWhen
            // and renders the chip natively at 60fps. nm.notify() only fires on state changes
            // (pause, cancel, +5, new timer) so card-swap transitions are never interrupted.
            var lastSnapshot = emptyMap<Long, Long>() // id -> endTimeMillis
            while (true) {
                val timers  = repository.getAllTimers().first()
                val running = timers.filter { it.endTimeMillis > 0 }
                if (running.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                val snapshot = running.associate { it.id to it.endTimeMillis }
                if (snapshot != lastSnapshot) {
                    nm.notify(NOTIF_ID, buildNotification(running))
                    lastSnapshot = snapshot
                }
                delay(1000L)
            }
        }
    }

    private fun buildSeedNotification(endTimeMillis: Long = 0L, label: String = ""): Notification {
        val displayLabel  = label.ifBlank { "Timer" }
        val whenMillis    = if (endTimeMillis > System.currentTimeMillis()) endTimeMillis
                            else System.currentTimeMillis() + 60_000L
        return NotificationCompat.Builder(this, HabitApp.TIMER_ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(displayLabel)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(whenMillis)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setContentIntent(makeTimerTabIntent())
            .build()
    }

    private fun buildPlaceholderNotification(): Notification {
        return NotificationCompat.Builder(this, HabitApp.TIMER_ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Timer")
            .setSubText("Timer")
            .setShowWhen(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setContentIntent(makeTimerTabIntent())
            .build()
    }

    private fun makeTimerTabIntent(): PendingIntent {
        val i = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TAB, "clock")
        }
        return PendingIntent.getActivity(
            this, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(runningTimers: List<Timer>): Notification {
        val tapIntent = makeTimerTabIntent()

        if (runningTimers.size == 1) {
            val timer = runningTimers[0]
            val label = if (timer.label.isNotBlank()) timer.label else "Timer"

            fun servicePi(requestOffset: Long, action: String) = PendingIntent.getService(
                this, (timer.id + requestOffset).toInt(),
                Intent(this, TimerForegroundService::class.java).apply {
                    this.action = action
                    putExtra(EXTRA_TIMER_ID, timer.id)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val cancelPi   = servicePi(10_000L, ACTION_CANCEL_TIMER)
            val pausePi    = servicePi(20_000L, ACTION_PAUSE_TIMER)
            val addTimePi  = servicePi(30_000L, ACTION_ADD_TIME)

            // setUsesChronometer + setWhen → Samsung renders chip AND card countdown natively
            // at 60fps. No setShortCriticalText = no override of Samsung's native chip renderer.
            return NotificationCompat.Builder(this, HabitApp.TIMER_ONGOING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(label)
                .setContentText("Ends at ${formatEndTime(timer.endTimeMillis)}")
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(timer.endTimeMillis)
                .setShowWhen(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setRequestPromotedOngoing(true)
                .setContentIntent(tapIntent)
                .addAction(0, "Cancel", cancelPi)
                .addAction(0, "Pause", pausePi)
                .addAction(0, "+5 min", addTimePi)
                .build()
        }

        // Multiple timers — InboxStyle anchored to the soonest-ending timer
        val earliest = runningTimers.minByOrNull { it.endTimeMillis } ?: return buildPlaceholderNotification()
        val style = NotificationCompat.InboxStyle()
        runningTimers.forEach { t ->
            val remaining = maxOf(0L, (t.endTimeMillis - System.currentTimeMillis()) / 1000)
            val h = remaining / 3600
            val m = (remaining % 3600) / 60
            val s = remaining % 60
            val time = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
            val lbl = if (t.label.isNotBlank()) t.label else "Timer"
            style.addLine("$lbl  $time")
        }

        return NotificationCompat.Builder(this, HabitApp.TIMER_ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("${runningTimers.size} timers")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(earliest.endTimeMillis)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setContentIntent(tapIntent)
            .setStyle(style)
            .build()
    }

    private fun formatEndTime(endMillis: Long): String {
        val h = java.util.Calendar.getInstance().apply { timeInMillis = endMillis }.get(java.util.Calendar.HOUR_OF_DAY)
        val m = java.util.Calendar.getInstance().apply { timeInMillis = endMillis }.get(java.util.Calendar.MINUTE)
        return "%d:%02d %s".format(
            if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM"
        )
    }

    private fun formatChipTime(endMillis: Long): String {
        val remaining = maxOf(0L, (endMillis - System.currentTimeMillis()) / 1000)
        val h = remaining / 3600
        val m = (remaining % 3600) / 60
        val s = remaining % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s)
               else       "%d:%02d".format(m, s)
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

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_STOP           = "STOP"
        const val ACTION_CANCEL_TIMER   = "CANCEL_TIMER"
        const val ACTION_PAUSE_TIMER    = "PAUSE_TIMER"
        const val ACTION_ADD_TIME       = "ADD_TIME"
        const val EXTRA_TIMER_ID        = "timer_id"
        const val EXTRA_END_TIME_MILLIS = "end_time_millis"
        const val EXTRA_TIMER_LABEL     = "timer_label"
        const val NOTIF_ID              = 1_000_000

        fun startIntent(ctx: Context, endTimeMillis: Long = 0L, label: String = "") =
            Intent(ctx, TimerForegroundService::class.java).apply {
                if (endTimeMillis > 0L) putExtra(EXTRA_END_TIME_MILLIS, endTimeMillis)
                if (label.isNotEmpty()) putExtra(EXTRA_TIMER_LABEL, label)
            }
        fun stopIntent(ctx: Context) =
            Intent(ctx, TimerForegroundService::class.java).apply { action = ACTION_STOP }
    }
}
