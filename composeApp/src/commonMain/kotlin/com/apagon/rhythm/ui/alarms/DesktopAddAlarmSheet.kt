package com.apagon.rhythm.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.platform.LocaleFormatting
import com.apagon.rhythm.ui.components.CrystalWindowContent
import com.apagon.rhythm.ui.components.crystalSheetColor
import org.koin.compose.koinInject

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

// Desktop counterpart to androidMain's AddAlarmSheet.kt (Stage 12) — not a
// literal move. The Android original pulls RingtoneManager, LocalContext for
// a system picker, and FluidTextField/MomentumButton/EditorialTitle
// (androidMain-only SharedComposables), so this is a new plain-M3 sheet
// following the DesktopAddCalendarEventSheet TimePicker pattern (Stage 8).
// Sound picking is out of scope — no desktop ringtone library exists yet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAddAlarmSheet(
    existing: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (label: String, hour: Int, minute: Int, repeatDaysMask: Int) -> Unit
) {
    val localeFormatting = koinInject<LocaleFormatting>()
    val is24Hour = remember(localeFormatting) { localeFormatting.is24HourFormat() }

    var label by remember { mutableStateOf(existing?.label ?: "") }
    var hour by remember { mutableIntStateOf(existing?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(existing?.minute ?: 0) }
    var repeatMask by remember { mutableIntStateOf(existing?.repeatDaysMask ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = crystalSheetColor(fallback = MaterialTheme.colorScheme.surface)
    ) {
        // Stage 14 invariant #2: this window is separate from the main one, so its inherited
        // blur field (if any) is unusable — CrystalWindowContent replaces it with a fresh one
        // scoped to this sheet.
        CrystalWindowContent {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                if (existing == null) "New Alarm" else "Edit Alarm",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Surface(
                onClick = { showTimePicker = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "%02d:%02d".format(hour, minute),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Repeat", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_LABELS.forEachIndexed { index, day ->
                        val bit = 1 shl index
                        val selected = (repeatMask and bit) != 0
                        Surface(
                            onClick = { repeatMask = repeatMask xor bit },
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    day.take(1),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
                Text(
                    if (repeatMask == 0) "One-time — auto-disables after it fires" else "Repeats weekly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { onSave(label, hour, minute, repeatMask) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
        }
        }
    }

    // Stage 14 gap, flagged not hidden: this AlertDialog is stock M3 and paints its own opaque
    // container from outside any content slot CrystalWindowContent could reach — Crystal-izing it
    // properly needs a custom dialog shell (the Android original's RhythmAlertDialog), which is
    // out of scope for this port pass. Renders as plain Material here under every theme style.
    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = is24Hour)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = state.hour
                    minute = state.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) }
        )
    }
}
