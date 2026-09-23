package com.apagon.rhythm.ui.alarms

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
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.apagon.rhythm.notifications.EXTRA_ALARM_ID
import com.apagon.rhythm.notifications.EXTRA_ALARM_LABEL
import com.apagon.rhythm.notifications.EXTRA_ALARM_HOUR
import com.apagon.rhythm.notifications.EXTRA_ALARM_MINUTE
import com.apagon.rhythm.notifications.EXTRA_REPEAT_DAYS
import com.apagon.rhythm.notifications.EXTRA_SOUND_URI
import com.apagon.rhythm.notifications.EXTRA_VIBRATION_PATTERN_ID
import com.apagon.rhythm.notifications.ReminderScheduler
import com.apagon.rhythm.notifications.TYPE_ALARM
import com.apagon.rhythm.ui.theme.RhythmTheme
import com.apagon.rhythm.ui.util.AlertVibrator
import com.apagon.rhythm.ui.util.formatTime
class AlarmAlertActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var alarmId: Long = -1L

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

        // Dismiss the keyguard so the user can interact without unlocking first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        }

        alarmId        = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val label      = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: ""
        val soundUri   = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""
        val vibrationPatternId = intent.getStringExtra(EXTRA_VIBRATION_PATTERN_ID) ?: "default"
        val repeatDays = intent.getIntExtra(EXTRA_REPEAT_DAYS, 0)
        val hour       = intent.getIntExtra(EXTRA_ALARM_HOUR, 0)
        val minute     = intent.getIntExtra(EXTRA_ALARM_MINUTE, 0)

        // Listen for external dismiss/snooze (notification action tapped while activity is showing)
        ContextCompat.registerReceiver(
            this, stopReceiver,
            IntentFilter("${ACTION_ALARM_STOP}_$alarmId"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        startSoundAndVibration(soundUri, vibrationPatternId)

        setContent {
            RhythmTheme {
                val is24Hour = remember { DateFormat.is24HourFormat(this) }
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
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ALARM",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 3.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatTime(hour, minute, is24Hour),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Light
                            )
                            if (label.isNotBlank()) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
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
                                    cancelNotification(alarmId)
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
                                    cancelNotification(alarmId)
                                    ReminderScheduler.scheduleSnooze(
                                        this@AlarmAlertActivity,
                                        TYPE_ALARM,
                                        alarmId, label, "",
                                        soundUri = soundUri, repeatDays = repeatDays,
                                        hour = hour, minute = minute
                                    )
                                    finishAndRemoveTask()
                                },
                                modifier = Modifier.size(84.dp),
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
                setDataSource(this@AlarmAlertActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fall back to system default alarm sound
            try {
                val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(this@AlarmAlertActivity, fallback)
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

    private fun cancelNotification(alarmId: Long) {
        val notifId = ((alarmId + ReminderScheduler.ALARM_ID_OFFSET) % Int.MAX_VALUE).toInt()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) { }
        stopSoundAndVibration()
        if (alarmId != -1L) cancelNotification(alarmId)
    }

    companion object {
        const val ACTION_ALARM_STOP = "com.apagon.rhythm.ACTION_ALARM_STOP"
    }
}
