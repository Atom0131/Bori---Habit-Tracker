package com.apagon.rhythm.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri

/**
 * Centralised keys, channel IDs, action strings, and helpers shared between
 * [ReminderScheduler], [ReminderReceiver], and [com.apagon.rhythm.HabitApp].
 */

// ── Notification channel IDs ──────────────────────────────────────────────
internal const val CHANNEL_ID        = "reminders_v4"
internal const val CHANNEL_SILENT_ID = "reminders_silent"

// ── Broadcast action strings ──────────────────────────────────────────────
internal const val ACTION_COMPLETE = "com.apagon.rhythm.ACTION_COMPLETE"
internal const val ACTION_SNOOZE   = "com.apagon.rhythm.ACTION_SNOOZE"
internal const val ACTION_DISMISS  = "com.apagon.rhythm.ACTION_DISMISS"

// ── Payload type discriminator ────────────────────────────────────────────
internal const val EXTRA_TYPE      = "type"
internal const val TYPE_HABIT      = "habit"
internal const val TYPE_REMINDER   = "reminder"
internal const val TYPE_ALARM      = "alarm"
internal const val TYPE_EVENT      = "event"
internal const val TYPE_TODO       = "todo"

// ── Common ────────────────────────────────────────────────────────────────
internal const val EXTRA_NOTIF_ID  = "notifId"
internal const val EXTRA_IS_SNOOZE = "isSnooze"
internal const val EXTRA_SOUND_URI = "soundUri"
internal const val EXTRA_VIBRATION_PATTERN_ID = "vibration_pattern_id"

// ── Habit notification extras ─────────────────────────────────────────────
internal const val EXTRA_HABIT_ID      = "habitId"
internal const val EXTRA_HABIT_NAME    = "habitName"
internal const val EXTRA_REMINDER_TIME = "reminderTime"

// ── One-shot reminder extras ──────────────────────────────────────────────
internal const val EXTRA_REMINDER_ID    = "reminderId"
internal const val EXTRA_REMINDER_TITLE = "reminderTitle"
internal const val EXTRA_REMINDER_NOTE  = "reminderNote"

// ── Alarm extras ──────────────────────────────────────────────────────────
internal const val EXTRA_ALARM_ID     = "alarmId"
internal const val EXTRA_ALARM_LABEL  = "label"
internal const val EXTRA_REPEAT_DAYS  = "repeatDays"
internal const val EXTRA_ALARM_HOUR   = "hour"
internal const val EXTRA_ALARM_MINUTE = "minute"

// ── Channel helper ────────────────────────────────────────────────────────

/**
 * Creates and registers a [NotificationChannel] if it does not already exist.
 */
internal fun ensureChannel(
    nm: NotificationManager,
    channelId: String,
    channelName: String,
    importance: Int,
    audioUsage: Int,
    sound: Uri?,
    vibrationPattern: LongArray
) {
    if (nm.getNotificationChannel(channelId) != null) return
    val audioAttributes = AudioAttributes.Builder()
        .setUsage(audioUsage)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    val channel = NotificationChannel(channelId, channelName, importance).apply {
        enableVibration(true)
        this.vibrationPattern = vibrationPattern
        enableLights(true)
        setSound(sound, audioAttributes)
    }
    nm.createNotificationChannel(channel)
}
