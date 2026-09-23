package com.apagon.rhythm.ui.reminders

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.apagon.rhythm.notifications.*
import com.apagon.rhythm.ui.theme.RhythmTheme
import com.apagon.rhythm.ui.util.AlertVibrator
class GenericAlertActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var alertId: Long = -1L
    private var alertType: String = ""

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            stopSoundAndVibration()
            finishAndRemoveTask()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Wake screen and keep it on above the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Dismiss the keyguard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        }

        alertType = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_REMINDER
        val type = alertType
        alertId = intent.getLongExtra(if (type == TYPE_HABIT) EXTRA_HABIT_ID else EXTRA_REMINDER_ID, -1L)
        val id = alertId
        val title = intent.getStringExtra(if (type == TYPE_HABIT) EXTRA_HABIT_NAME else EXTRA_REMINDER_TITLE) ?: "Reminder"
        val note = intent.getStringExtra(if (type == TYPE_HABIT) EXTRA_REMINDER_TIME else EXTRA_REMINDER_NOTE) ?: ""
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""
        val vibrationPatternId = intent.getStringExtra(EXTRA_VIBRATION_PATTERN_ID) ?: "default"

        val actionId = if (type == TYPE_HABIT) habitActionId(id) else reminderActionId(id)

        ContextCompat.registerReceiver(
            this, stopReceiver,
            IntentFilter("${ACTION_STOP}_$actionId"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        cancelNotification(type, id)
        startSoundAndVibration(soundUri, vibrationPatternId)

        setContent {
            RhythmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(40.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when(type) {
                                    TYPE_HABIT -> "HABIT REMINDER"
                                    TYPE_TODO -> "TO-DO REMINDER"
                                    else -> "REMINDER"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 3.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (note.isNotBlank()) {
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconButton(
                                onClick = {
                                    stopSoundAndVibration()
                                    
                                    // Send broadcast to complete in background
                                    val intent = Intent(this@GenericAlertActivity, ReminderReceiver::class.java).apply {
                                        action = ReminderReceiver.ACTION_COMPLETE
                                        putExtra(ReminderReceiver.EXTRA_TYPE, type)
                                        putExtra(ReminderReceiver.EXTRA_NOTIF_ID, intent.getIntExtra(ReminderReceiver.EXTRA_NOTIF_ID, -1))
                                        if (type == TYPE_HABIT) {
                                            putExtra(ReminderReceiver.EXTRA_HABIT_ID, id)
                                        } else {
                                            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
                                        }
                                    }
                                    sendBroadcast(intent)
                                    
                                    cancelNotification(type, id)
                                    finishAndRemoveTask()
                                },
                                modifier = Modifier.size(84.dp),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            
                            FilledIconButton(
                                onClick = {
                                    stopSoundAndVibration()
                                    cancelNotification(type, id)
                                    ReminderScheduler.scheduleSnooze(
                                        this@GenericAlertActivity,
                                        type, id, title, note,
                                        soundUri = soundUri
                                    )
                                    finishAndRemoveTask()
                                },
                                modifier = Modifier.size(84.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Snooze,
                                    contentDescription = "Snooze 5 min",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun habitActionId(habitId: Long) = (habitId % Int.MAX_VALUE).toInt()
    private fun reminderActionId(reminderId: Long) = ((reminderId + ReminderScheduler.REMINDER_ID_OFFSET) % Int.MAX_VALUE).toInt()

    private fun startSoundAndVibration(soundUri: String, vibrationPatternId: String = "default") {
        AlertVibrator.start(this, vibrationPatternId)
        if (soundUri == "silent") return
        val uri: Uri = if (soundUri.isEmpty()) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } else {
            Uri.parse(soundUri)
        }
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(this@GenericAlertActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            try {
                val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(this@GenericAlertActivity, fallback)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (ignored: Exception) { }
        }
    }

    private fun stopSoundAndVibration() {
        AlertVibrator.stop(this)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun cancelNotification(type: String, id: Long) {
        val notifId = if (type == TYPE_HABIT) habitActionId(id) else reminderActionId(id)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) { }
        stopSoundAndVibration()
        if (alertId != -1L) cancelNotification(alertType, alertId)
    }

    companion object {
        const val ACTION_STOP = "com.apagon.rhythm.ACTION_GENERIC_STOP"
    }
}
