package com.apagon.rhythm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.setWidgetPreviews
import com.apagon.rhythm.data.CleanupManager
import com.apagon.rhythm.notifications.ReminderReceiver
import com.apagon.rhythm.widget.ActiveTimerWidgetReceiver
import com.apagon.rhythm.widget.HabitGridWidgetReceiver
import com.apagon.rhythm.widget.HabitRingWidgetReceiver
import com.apagon.rhythm.widget.StreakSpotlightWidgetReceiver
import com.apagon.rhythm.widget.TodayGlanceWidgetReceiver
import com.apagon.rhythm.widget.WidgetUpdateWorker
import com.apagon.rhythm.widget.refreshAllWidgets
import com.apagon.rhythm.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class HabitApp : Application() {

    // Application-scoped coroutine scope — survives config changes, cancelled on process death
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HabitApp)
            modules(appModule)
        }
        CrashLogger.install(this)
        createNotificationChannels()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "widget_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
        )

        applicationScope.launch {
            try {
                get<CleanupManager>().purgeAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Force an immediate redraw of every placed widget on app launch — the periodic
        // WorkManager refresh (below, every 15 min) doesn't fire right away when re-enqueued,
        // so without this, existing widgets keep showing a stale render after any app update.
        applicationScope.launch {
            try {
                refreshAllWidgets(this@HabitApp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Generate + publish real, filled-in widget-picker preview thumbnails.
        // Android 15+ only — older devices keep using the static XML previewLayout fallback.
        // Result is logged (not just exceptions) since setWidgetPreviews() is @CheckResult and
        // can silently no-op (e.g. rate-limited) without throwing — see tag "RhythmWidgetPreviews".
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            applicationScope.launch {
                val manager = GlanceAppWidgetManager(this@HabitApp)
                logPreviewResult("HabitRing") { manager.setWidgetPreviews<HabitRingWidgetReceiver>() }
                logPreviewResult("HabitGrid") { manager.setWidgetPreviews<HabitGridWidgetReceiver>() }
                logPreviewResult("TodayGlance") { manager.setWidgetPreviews<TodayGlanceWidgetReceiver>() }
                logPreviewResult("StreakSpotlight") { manager.setWidgetPreviews<StreakSpotlightWidgetReceiver>() }
                logPreviewResult("ActiveTimer") { manager.setWidgetPreviews<ActiveTimerWidgetReceiver>() }
            }
        }
    }

    private suspend fun logPreviewResult(widgetName: String, call: suspend () -> Int) {
        try {
            val result = call()
            // GlanceAppWidgetManager.SetWidgetPreviewsResult: SUCCESS = 0
            if (result == 0) {
                Log.d(PREVIEW_LOG_TAG, "$widgetName: preview published successfully (result=$result)")
            } else {
                Log.w(PREVIEW_LOG_TAG, "$widgetName: preview NOT published — non-success result=$result (likely rate-limited)")
            }
        } catch (e: Exception) {
            Log.e(PREVIEW_LOG_TAG, "$widgetName: setWidgetPreviews threw", e)
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val habitAudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // 1. Habit Reminders — alarm stream so it rings even with notification volume muted
        val habitsChannel = NotificationChannel(
            ReminderReceiver.CHANNEL_ID,
            "Habit Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for your habits and general tasks"
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), habitAudioAttributes)
        }
        nm.createNotificationChannel(habitsChannel)

        // 2. Alarms (highest importance) — sound comes from AlarmAlertActivity MediaPlayer
        // Note: createNotificationChannel() is idempotent — it will NOT reset user preferences
        // if the channel already exists. Never call deleteNotificationChannel() here as it
        // destroys user-customized settings (importance, vibration, sound toggles).
        val alarmsChannel = NotificationChannel(
            ReminderReceiver.CHANNEL_ID_ALARM,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High priority alarms"
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            setSound(null, null)
        }
        nm.createNotificationChannel(alarmsChannel)

        // 3. Timers (Completion) — sound comes from TimerAlertActivity MediaPlayer
        val timerChannel = NotificationChannel(
            TIMER_CHANNEL_ID,
            "Timers",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Timer completion alerts"
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            setSound(null, null)
        }
        nm.createNotificationChannel(timerChannel)

        // 3b. Reminder Alerts — alarm-stream audio so alerts make sound even if full-screen intent fails
        val alarmAudioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val reminderAlertChannel = NotificationChannel(
            ReminderReceiver.CHANNEL_ID_REMINDER_ALERT,
            "Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders and to-do alerts"
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmAudioAttr)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            setShowBadge(true)
        }
        nm.createNotificationChannel(reminderAlertChannel)

        // 4. Timer Ongoing (Progress) — IMPORTANCE_DEFAULT required for Samsung Now Bar.
        // Sound/vibration suppressed so it stays silent despite the higher importance.
        val timerOngoingChannel = NotificationChannel(
            TIMER_ONGOING_CHANNEL_ID,
            "Timer Running",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows active countdown timers"
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannel(timerOngoingChannel)

        // 5. Silent Channel
        val silentChannel = NotificationChannel(
            ReminderReceiver.CHANNEL_ID_SILENT,
            "Silent Notifications",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications without sound or vibration"
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannel(silentChannel)

        // 6. Silent High Importance Channel — vibration required for Android 16 heads-up
        val silentHighChannel = NotificationChannel(
            ReminderReceiver.CHANNEL_ID_SILENT_HIGH,
            "Silent Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts without ringtone — vibrates only"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            setShowBadge(true)
        }
        nm.createNotificationChannel(silentHighChannel)
    }

    companion object {
        const val TIMER_CHANNEL_ID = "timers_v2"
        const val TIMER_ONGOING_CHANNEL_ID = "timer_ongoing_v2"
        val VIBRATION_PATTERN = longArrayOf(0, 300, 200, 300)
        private const val PREVIEW_LOG_TAG = "RhythmWidgetPreviews"
    }
}
