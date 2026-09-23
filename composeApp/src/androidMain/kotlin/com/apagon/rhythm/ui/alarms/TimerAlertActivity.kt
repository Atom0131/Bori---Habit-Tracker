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
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.apagon.rhythm.notifications.TimerCompletionReceiver
import com.apagon.rhythm.ui.theme.RhythmTheme
import com.apagon.rhythm.ui.util.AlertVibrator
class TimerAlertActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            stopSoundAndVibration()
            finishAndRemoveTask()
        }
    }

    private fun sendRestartBroadcast(timerId: Long) {
        sendBroadcast(
            Intent(this, TimerCompletionReceiver::class.java).apply {
                action = TimerCompletionReceiver.ACTION_RESTART
                putExtra(TimerCompletionReceiver.EXTRA_TIMER_ID, timerId)
            }
        )
    }

    private fun sendPomoAdvanceBroadcast(timerId: Long) {
        sendBroadcast(
            Intent(this, TimerCompletionReceiver::class.java).apply {
                action = TimerCompletionReceiver.ACTION_POMO_ADVANCE
                putExtra(TimerCompletionReceiver.EXTRA_TIMER_ID, timerId)
            }
        )
    }

    private fun sendAdd5MinBroadcast(timerId: Long) {
        sendBroadcast(
            Intent(this, TimerCompletionReceiver::class.java).apply {
                action = TimerCompletionReceiver.ACTION_ADD_5_MIN
                putExtra(TimerCompletionReceiver.EXTRA_TIMER_ID, timerId)
            }
        )
    }

    private var timerId: Long = -1L
    private var label: String = ""
    private var soundUri: String = ""
    private var isPomoDone: Boolean = false
    private var pomoPhase: String = ""
    private var durationSeconds: Int = 0

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateFromIntent(intent)
    }

    private fun updateFromIntent(intent: Intent) {
        // Unregister old receiver if timerId changed
        if (timerId != -1L) {
            try { unregisterReceiver(stopReceiver) } catch (_: Exception) {}
        }

        timerId         = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
        label           = intent.getStringExtra(EXTRA_TIMER_LABEL) ?: ""
        soundUri        = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""
        isPomoDone      = intent.getBooleanExtra(EXTRA_POMO_ADVANCE, false)
        pomoPhase       = intent.getStringExtra("pomo_phase") ?: ""
        durationSeconds = intent.getIntExtra(EXTRA_TIMER_DURATION, 0)
        val vibrationPatternId = intent.getStringExtra(com.apagon.rhythm.notifications.ReminderReceiver.EXTRA_VIBRATION_PATTERN_ID) ?: "default"

        ContextCompat.registerReceiver(
            this, stopReceiver,
            IntentFilter("${ACTION_TIMER_STOP}_$timerId"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        stopSoundAndVibration()
        startSoundAndVibration(soundUri, vibrationPatternId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Wake screen and show above lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Dismiss keyguard so user can interact without unlocking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        }

        updateFromIntent(intent)

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
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when {
                                    isPomoDone && pomoPhase == "WORK" -> "POMODORO — FOCUS"
                                    isPomoDone && (pomoPhase == "SHORT_BREAK" || pomoPhase == "LONG_BREAK") -> "POMODORO — BREAK"
                                    isPomoDone -> "POMODORO"
                                    else -> "TIMER"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 3.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (durationSeconds > 0) {
                                Text(
                                    text = formatDuration(durationSeconds),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Light
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = when {
                                    isPomoDone && pomoPhase == "WORK" -> "Focus Session Done!"
                                    isPomoDone && (pomoPhase == "SHORT_BREAK" || pomoPhase == "LONG_BREAK") -> "Break Finished!"
                                    isPomoDone -> "Pomodoro Phase Done!"
                                    else -> "Timer Done!"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Light
                            )

                            val subtext = when {
                                isPomoDone && pomoPhase == "WORK" -> "Time for a break. You earned it!"
                                isPomoDone && (pomoPhase == "SHORT_BREAK" || pomoPhase == "LONG_BREAK") -> "Break is over. Let's get back to work!"
                                else -> label
                            }

                            if (subtext.isNotBlank()) {
                                Text(
                                    text = subtext,
                                    style = if (isPomoDone) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineSmall,
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
                                    cancelNotification(timerId)
                                    if (isPomoDone) sendPomoAdvanceBroadcast(timerId)
                                    finishAndRemoveTask()
                                },
                                modifier = Modifier.size(84.dp),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            
                            FilledIconButton(
                                onClick = {
                                    stopSoundAndVibration()
                                    cancelNotification(timerId)
                                    sendAdd5MinBroadcast(timerId)
                                    finishAndRemoveTask()
                                },
                                modifier = Modifier.size(84.dp),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAlarm,
                                    contentDescription = "Add 5 minutes",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            
                            FilledIconButton(
                                onClick = {
                                    stopSoundAndVibration()
                                    cancelNotification(timerId)
                                    sendRestartBroadcast(timerId)
                                    finishAndRemoveTask()
                                },
                                modifier = Modifier.size(84.dp),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restart",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun startSoundAndVibration(soundUri: String, vibrationPatternId: String = "default") {
        AlertVibrator.start(this, vibrationPatternId)
        if (soundUri == "silent") return
        val uri: Uri = when {
            soundUri.isEmpty() -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            else -> Uri.parse(soundUri)
        }
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(this@TimerAlertActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            try {
                val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(this@TimerAlertActivity, fallback)
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

    private fun cancelNotification(timerId: Long) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIF_ID_BASE + timerId.toInt())
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) { }
        stopSoundAndVibration()
        if (timerId != -1L) cancelNotification(timerId)
    }

    companion object {
        const val ACTION_TIMER_STOP    = "com.apagon.rhythm.ACTION_TIMER_STOP"
        const val EXTRA_TIMER_ID       = "timer_alert_id"
        const val EXTRA_TIMER_LABEL    = "timer_alert_label"
        const val EXTRA_TIMER_DURATION = "timer_alert_duration"
        const val EXTRA_SOUND_URI      = "timer_alert_sound"
        const val EXTRA_POMO_ADVANCE   = "timer_alert_pomo_advance"
        const val NOTIF_ID_BASE        = 900_000
    }
}
