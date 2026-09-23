package com.apagon.rhythm.ui.alarms

import android.media.RingtoneManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.ui.util.*
import com.apagon.rhythm.ui.util.EditorialTitle
import com.apagon.rhythm.ui.util.FluidTextField
import com.apagon.rhythm.ui.util.MomentumButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimerSheet(
    onDismiss: () -> Unit,
    initialIsPomo: Boolean = false,
    existing: Timer? = null,
    isPro: Boolean = true,
    onShowPaywall: (String) -> Unit = {},
    onSave: (
        label: String,
        durationSeconds: Int,
        soundUri: String,
        vibrationPatternId: String,
        isPomo: Boolean,
        pomoWorkSecs: Int,
        pomoShortBreakSecs: Int,
        pomoLongBreakSecs: Int,
        pomoSessionsPerRound: Int
    ) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isPomo by remember { mutableStateOf(existing?.isPomo ?: initialIsPomo) }
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var selectedSoundUri by remember { mutableStateOf(existing?.soundUri ?: "") }
    var silentAlarm by remember { mutableStateOf(existing?.soundUri == "silent") }
    var selectedPatternId by remember { mutableStateOf(existing?.vibrationPatternId ?: "default") }

    // Regular timer
    val existingDuration = existing?.durationSeconds ?: 0
    var timerH by remember { mutableIntStateOf(existingDuration / 3600) }
    var timerM by remember { mutableIntStateOf((existingDuration % 3600) / 60) }
    var timerS by remember { mutableIntStateOf(existingDuration % 60) }

    // Pomodoro
    val eWork = existing?.pomoWorkSecs ?: 1500
    val eShort = existing?.pomoShortBreakSecs ?: 300
    val eLong = existing?.pomoLongBreakSecs ?: 900
    
    var pomoWorkH by remember { mutableIntStateOf(eWork / 3600) }
    var pomoWorkM by remember { mutableIntStateOf((eWork % 3600) / 60) }
    var pomoWorkS by remember { mutableIntStateOf(eWork % 60) }
    var pomoShortH by remember { mutableIntStateOf(eShort / 3600) }
    var pomoShortM by remember { mutableIntStateOf((eShort % 3600) / 60) }
    var pomoShortS by remember { mutableIntStateOf(eShort % 60) }
    var pomoLongH by remember { mutableIntStateOf(eLong / 3600) }
    var pomoLongM by remember { mutableIntStateOf((eLong % 3600) / 60) }
    var pomoLongS by remember { mutableIntStateOf(eLong % 60) }
    var pomoSessions by remember { mutableIntStateOf(existing?.pomoSessionsPerRound ?: 4) }

    val totalSeconds = timerH * 3600 + timerM * 60 + timerS
    val pomoWorkSecs = pomoWorkH * 3600 + pomoWorkM * 60 + pomoWorkS
    val pomoShortSecs = pomoShortH * 3600 + pomoShortM * 60 + pomoShortS
    val pomoLongSecs = pomoLongH * 3600 + pomoLongM * 60 + pomoLongS

    val canSave = if (isPomo) pomoWorkSecs > 0 && pomoShortSecs > 0 && pomoLongSecs > 0
                  else totalSeconds > 0

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
                text = if (existing != null) {
                    if (isPomo) "Edit Pomodoro" else "Edit Timer"
                } else {
                    if (isPomo) "New Pomodoro" else "New Timer"
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Mode toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isPomo,
                    onClick = { 
                        focusManager.clearFocus()
                        isPomo = false 
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Timer") }
                SegmentedButton(
                    selected = isPomo,
                    onClick = { 
                        focusManager.clearFocus()
                        if (isPro) {
                            isPomo = true
                        } else {
                            onShowPaywall("Exclusive Pomodoro Focus Timers are a Pro feature!")
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Pomodoro") }
            }

            AnimatedContent(
                targetState = isPomo,
                transitionSpec = { fadeIn(tween(250, easing = EaseInOut)) togetherWith fadeOut(tween(200, easing = EaseInOut)) },
                label = "timerModeContent"
            ) { pomodoro ->
            if (!pomodoro) {
                // Regular Timer Card
                Text(
                    text = "DURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MaterialDurationPicker(
                    hours = timerH,
                    minutes = timerM,
                    seconds = timerS,
                    onChanged = { h, m, s -> timerH = h; timerM = m; timerS = s }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val tabs = listOf("Work", "Short Break", "Long Break")
                    var selectedTabIndex by remember { mutableIntStateOf(0) }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "pomodoroTabContent"
                    ) { tabIndex ->
                    when (tabIndex) {
                        0 -> MaterialDurationPicker(
                            hours = pomoWorkH, minutes = pomoWorkM, seconds = pomoWorkS,
                            onChanged = { h, m, s -> pomoWorkH = h; pomoWorkM = m; pomoWorkS = s }
                        )
                        1 -> MaterialDurationPicker(
                            hours = pomoShortH, minutes = pomoShortM, seconds = pomoShortS,
                            onChanged = { h, m, s -> pomoShortH = h; pomoShortM = m; pomoShortS = s }
                        )
                        2 -> MaterialDurationPicker(
                            hours = pomoLongH, minutes = pomoLongM, seconds = pomoLongS,
                            onChanged = { h, m, s -> pomoLongH = h; pomoLongM = m; pomoLongS = s }
                        )
                        else -> Unit
                    }
                    } // end AnimatedContent (tab)

                    // Sessions per round stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SESSIONS PER ROUND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (pomoSessions > 1) pomoSessions-- },
                                enabled = pomoSessions > 1
                            ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                            Text(
                                text = pomoSessions.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(
                                onClick = { if (pomoSessions < 12) pomoSessions++ },
                                enabled = pomoSessions < 12
                            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }
            }
            } // end AnimatedContent (mode)

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

            MomentumButton(
                text = if (existing != null) "Update" else if (isPomo) "Create Pomodoro" else "Create Timer",
                onClick = {
                    onSave(
                        label.trim(),
                        if (isPomo) pomoWorkSecs else totalSeconds,
                        selectedSoundUri,
                        selectedPatternId,
                        isPomo,
                        pomoWorkSecs,
                        pomoShortSecs,
                        pomoLongSecs,
                        pomoSessions
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
