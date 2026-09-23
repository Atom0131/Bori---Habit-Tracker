package com.apagon.rhythm.ui.alarms

import android.media.RingtoneManager
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.ui.util.*
import com.apagon.rhythm.ui.util.EditorialTitle
import com.apagon.rhythm.ui.util.FluidTextField
import com.apagon.rhythm.ui.util.MomentumButton
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAlarmSheet(
    existing: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (label: String, hour: Int, minute: Int, repeatDays: Int, soundUri: String, vibrationPatternId: String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val is24Hour = remember { context.isSystem24Hour() }

    var label by remember { mutableStateOf(existing?.label ?: "") }
    var repeatDays by remember { mutableIntStateOf(existing?.repeatDaysMask ?: 0) }
    var selectedSoundUri by remember { mutableStateOf(existing?.soundUri ?: "") }
    var silentAlarm by remember { mutableStateOf(existing?.soundUri == "silent") }
    var selectedPatternId by remember { mutableStateOf(existing?.vibrationPatternId ?: "default") }

    var selectedHour by remember { mutableStateOf(existing?.hour ?: 8) }
    var selectedMinute by remember { mutableStateOf(existing?.minute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .blockSheetBoundaryOverscroll()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            EditorialTitle(
                text = if (existing != null) "Edit Alarm" else "New Alarm",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Time Picker Trigger Card
            Surface(
                onClick = { showTimePicker = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 2.dp,
                tonalElevation = 1.dp,
                border = BorderStroke(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AccessTime, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = formatTime(selectedHour, selectedMinute, is24Hour),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            FluidTextField(
                value = label,
                onValueChange = { label = it },
                label = "LABEL (OPTIONAL)",
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("Silent Alarm", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Vibrates only — ignores phone sound settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = silentAlarm,
                    onCheckedChange = {
                        silentAlarm = it
                        selectedSoundUri = if (it) "silent" else ""
                    }
                )
            }
            if (!silentAlarm) {
                SoundPickerButton(
                    soundUri = selectedSoundUri,
                    ringtoneType = RingtoneManager.TYPE_ALARM,
                    onSoundSelected = { selectedSoundUri = it }
                )
            }
            VibrationPatternButton(
                patternId = selectedPatternId,
                onPatternSelected = { selectedPatternId = it },
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "REPEAT DAYS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                @Composable
                fun DayCircle(index: Int, dayLabel: String) {
                    val selected = (repeatDays and (1 shl index)) != 0
                    Surface(
                        onClick = {
                            repeatDays = if (selected) {
                                repeatDays and (1 shl index).inv()
                            } else {
                                repeatDays or (1 shl index)
                            }
                        },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shadowElevation = if (selected) 4.dp else 2.dp,
                        tonalElevation = if (selected) 2.dp else 1.dp,
                        border = if (selected) null else BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        (0..3).forEach { index -> DayCircle(index, DAY_NAMES_SINGLE[index]) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        (4..6).forEach { index -> DayCircle(index, DAY_NAMES_SINGLE[index]) }
                    }
                }
            }

            MomentumButton(
                text = if (existing != null) "Save Changes" else "Create Alarm",
                onClick = {
                    if (!hasTechnicalPermissions(context)) {
                        showPermissionDialog = true
                        return@MomentumButton
                    }
                    onSave(label.trim(), selectedHour, selectedMinute, repeatDays, selectedSoundUri, selectedPatternId)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showPermissionDialog) {
        TechnicalPermissionDialog(
            context = context,
            onDismiss = { showPermissionDialog = false }
        )
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = is24Hour
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    selectedHour = timeState.hour
                    selectedMinute = timeState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timeState)
                }
            }
        )
    }
}
