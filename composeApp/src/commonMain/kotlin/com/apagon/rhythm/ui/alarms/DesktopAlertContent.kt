package com.apagon.rhythm.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.platform.FiredAlert
import com.apagon.rhythm.platform.FiredAlertKind

// Content for Stage 12's always-on-top alert window (main.kt) — the desktop
// stand-in for Android's full-screen AlarmAlertActivity/TimerAlertActivity/
// GenericAlertActivity, which rely on KeyguardManager/WindowManager with no
// desktop equivalent. Per the user's own scope call: no wake-up-check dismiss
// puzzle (mobile-specific anti-oversleep mechanic) — just Dismiss, plus
// "Start Next Phase" for a completed Pomodoro work/break segment.
@Composable
fun DesktopAlertContent(
    alert: FiredAlert,
    onDismiss: () -> Unit,
    onStartNextPhase: (() -> Unit)?
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (alert.kind) {
                        FiredAlertKind.ALARM -> "Alarm"
                        FiredAlertKind.REMINDER -> "Reminder"
                        FiredAlertKind.TIMER -> "Timer"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(alert.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (alert.subtitle.isNotBlank()) {
                    Text(alert.subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onStartNextPhase != null) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Dismiss") }
                    Button(onClick = onStartNextPhase, modifier = Modifier.weight(1f)) { Text("Start Next Phase") }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors()
                    ) { Text("Dismiss") }
                }
            }
        }
    }
}
