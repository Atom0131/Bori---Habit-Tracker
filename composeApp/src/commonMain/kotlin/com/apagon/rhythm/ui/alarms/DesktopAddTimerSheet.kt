package com.apagon.rhythm.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.ui.components.CrystalWindowContent
import com.apagon.rhythm.ui.components.crystalSheetColor

// Desktop counterpart to androidMain's AddTimerSheet.kt (Stage 12) — new
// plain-M3 sheet, not a move: the Android original uses compose.animation
// (not a commonMain dependency, per Stage 9's own finding) plus
// FluidTextField/MomentumButton/EditorialTitle. Duration entry is
// minutes-only (no separate h/m/s fields) to keep this simple; Pomodoro
// fields are plain minute inputs converted to seconds on save.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAddTimerSheet(
    existing: Timer? = null,
    onDismiss: () -> Unit,
    onSave: (label: String, durationSeconds: Int) -> Unit,
    onSavePomo: (label: String, workMin: Int, shortBreakMin: Int, longBreakMin: Int, sessions: Int) -> Unit
) {
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var minutes by remember { mutableStateOf(((existing?.durationSeconds ?: 300) / 60).toString()) }
    var isPomo by remember { mutableStateOf(existing?.isPomo ?: false) }
    var workMin by remember { mutableStateOf(((existing?.pomoWorkSecs ?: 1500) / 60).toString()) }
    var shortBreakMin by remember { mutableStateOf(((existing?.pomoShortBreakSecs ?: 300) / 60).toString()) }
    var longBreakMin by remember { mutableStateOf(((existing?.pomoLongBreakSecs ?: 900) / 60).toString()) }
    var sessions by remember { mutableStateOf((existing?.pomoSessionsPerRound ?: 4).toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = crystalSheetColor(fallback = MaterialTheme.colorScheme.surface)
    ) {
        CrystalWindowContent {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (existing == null) "New Timer" else "Edit Timer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pomodoro", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isPomo, onCheckedChange = { isPomo = it })
            }

            if (isPomo) {
                OutlinedTextField(
                    value = workMin, onValueChange = { workMin = it }, label = { Text("Work (min)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shortBreakMin, onValueChange = { shortBreakMin = it }, label = { Text("Short break (min)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = longBreakMin, onValueChange = { longBreakMin = it }, label = { Text("Long break (min)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sessions, onValueChange = { sessions = it }, label = { Text("Sessions per round") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = minutes, onValueChange = { minutes = it }, label = { Text("Duration (min)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    if (isPomo) {
                        onSavePomo(
                            label,
                            workMin.toIntOrNull() ?: 25,
                            shortBreakMin.toIntOrNull() ?: 5,
                            longBreakMin.toIntOrNull() ?: 15,
                            sessions.toIntOrNull() ?: 4
                        )
                    } else {
                        onSave(label, (minutes.toIntOrNull() ?: 5) * 60)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
        }
        }
    }
}
