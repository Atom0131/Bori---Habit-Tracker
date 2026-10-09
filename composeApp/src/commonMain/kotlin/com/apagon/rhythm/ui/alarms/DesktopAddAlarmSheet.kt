package com.apagon.rhythm.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.foundation.clickable
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalChipSurface
import com.apagon.rhythm.ui.components.crystalControlSurface
import com.apagon.rhythm.ui.components.crystalSelectedChipColor
import com.apagon.rhythm.ui.components.crystalSelectedChipContentColor
import com.apagon.rhythm.ui.components.crystalTextFieldColors
import com.apagon.rhythm.ui.components.crystalTextFieldShape
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import com.apagon.rhythm.ui.util.RhythmSheet
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.Icons
import com.apagon.rhythm.ui.util.formatClockTime
import com.apagon.rhythm.ui.util.DayCircle
import com.apagon.rhythm.ui.util.PickerSummaryCard
import com.apagon.rhythm.ui.util.MomentumButton
import com.apagon.rhythm.ui.util.FluidTextField
import com.apagon.rhythm.ui.util.EditorialTitle

// bit0=Sun, bit1=Mon, ... bit6=Sat — matches Alarm.kt's repeatDaysMask KDoc (Android's convention).
private val DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

// Desktop counterpart to Android's AddAlarmSheet.kt, built from the same editor pieces
// (ui/util/EditorWidgets.kt). Sound picking is out of scope: no desktop ringtone library yet.
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

    // Android's AddAlarmSheet layout. Its sound, vibration and wake-up-check pickers are phone-only
    // (no desktop ringtone or vibration API); an edit keeps whatever the phone set.
    RhythmSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            EditorialTitle(if (existing != null) "Edit Alarm" else "New Alarm", modifier = Modifier.padding(bottom = 8.dp))

            PickerSummaryCard(icon = Icons.Default.AccessTime, value = formatClockTime(hour, minute, is24Hour), onClick = { showTimePicker = true })

            FluidTextField(value = label, onValueChange = { label = it }, label = "LABEL (OPTIONAL)")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("REPEAT DAYS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Android's two rows of 52dp circles, S M T W / T F S (bit 0 = Sunday for alarms).
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0..3, 4..6).forEach { range ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                            range.forEach { index ->
                                val bit = 1 shl index
                                DayCircle(
                                    label = DAY_LABELS[index].take(1),
                                    selected = (repeatMask and bit) != 0,
                                    onClick = { repeatMask = repeatMask xor bit },
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }
                    }
                }
            }

            MomentumButton(
                text = if (existing != null) "Save Changes" else "Create Alarm",
                onClick = { onSave(label.trim(), hour, minute, repeatMask) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = is24Hour)
        RhythmAlertDialog(
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
