package com.apagon.rhythm.ui.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.data.repository.TimerRepository
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.notifications.TimerCompletionReceiver
import com.apagon.rhythm.notifications.TimerForegroundService
import com.apagon.rhythm.widget.refreshAllWidgets
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimerUiState(
    val timer: Timer,
    val isRunning: Boolean,
    val displayRemaining: Int,
    val pomoPhaseLabel: String? = null  // e.g. "Work 2/4", "Short Break", "Long Break"
)
class TimerViewModel constructor(
    private val repository: TimerRepository,
    private val themePreferences: ThemePreferences,
    private val context: Context
) : ViewModel() {

    val isPro: StateFlow<Boolean> = themePreferences.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // Ticks every 500ms to drive UI recomposition while timers are running
    private val _tickMs = MutableStateFlow(System.currentTimeMillis())

    private val timers = repository.getAllTimers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timerUiStates = combine(timers, _tickMs) { timers, now ->
        timers.map { timer ->
            val isRunning = timer.endTimeMillis > 0
            val displayRemaining = if (isRunning) {
                maxOf(0, ((timer.endTimeMillis - now) / 1000).toInt())
            } else {
                timer.remainingSeconds
            }
            val pomoPhaseLabel = if (timer.isPomo) {
                when (timer.pomoPhase) {
                    "WORK"        -> "Work ${timer.pomoCurrentSession}/${timer.pomoSessionsPerRound}"
                    "SHORT_BREAK" -> "Short Break"
                    "LONG_BREAK"  -> "Long Break"
                    else          -> null
                }
            } else null
            TimerUiState(timer = timer, isRunning = isRunning, displayRemaining = displayRemaining, pomoPhaseLabel = pomoPhaseLabel)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Keep tick running so UI stays accurate while any timer is active
        viewModelScope.launch {
            while (true) {
                _tickMs.value = System.currentTimeMillis()
                delay(500L)
            }
        }
    }

    fun addTimer(label: String, durationSeconds: Int, soundUri: String = "", vibrationPatternId: String = "default") {
        viewModelScope.launch {
            repository.addTimer(
                Timer(label = label, durationSeconds = durationSeconds, remainingSeconds = durationSeconds, soundUri = soundUri, vibrationPatternId = vibrationPatternId)
            )
        }
    }

    fun addPomoTimer(
        label: String,
        workSecs: Int,
        shortBreakSecs: Int,
        longBreakSecs: Int,
        sessionsPerRound: Int,
        soundUri: String = "",
        vibrationPatternId: String = "default"
    ) {
        viewModelScope.launch {
            repository.addTimer(
                Timer(
                    label = label,
                    durationSeconds = workSecs,
                    remainingSeconds = workSecs,
                    soundUri = soundUri,
                    vibrationPatternId = vibrationPatternId,
                    isPomo = true,
                    pomoWorkSecs = workSecs,
                    pomoShortBreakSecs = shortBreakSecs,
                    pomoLongBreakSecs = longBreakSecs,
                    pomoSessionsPerRound = sessionsPerRound,
                    pomoCurrentSession = 1,
                    pomoPhase = "WORK"
                )
            )
        }
    }

    fun updateTimer(id: Long, label: String, durationSeconds: Int, soundUri: String = "", vibrationPatternId: String = "default") {
        viewModelScope.launch {
            val existing = timers.value.find { it.id == id } ?: return@launch
            // If it's not running, we update remainingSeconds too
            val newRemaining = if (existing.endTimeMillis == 0L) durationSeconds else existing.remainingSeconds
            repository.updateTimer(
                existing.copy(
                    label = label,
                    durationSeconds = durationSeconds,
                    remainingSeconds = newRemaining,
                    soundUri = soundUri,
                    vibrationPatternId = vibrationPatternId
                )
            )
        }
    }

    fun updatePomoTimer(
        id: Long,
        label: String,
        workSecs: Int,
        shortBreakSecs: Int,
        longBreakSecs: Int,
        sessionsPerRound: Int,
        soundUri: String = "",
        vibrationPatternId: String = "default"
    ) {
        viewModelScope.launch {
            val existing = timers.value.find { it.id == id } ?: return@launch
            val newRemaining = if (existing.endTimeMillis == 0L) workSecs else existing.remainingSeconds
            repository.updateTimer(
                existing.copy(
                    label = label,
                    durationSeconds = workSecs,
                    remainingSeconds = newRemaining,
                    soundUri = soundUri,
                    vibrationPatternId = vibrationPatternId,
                    isPomo = true,
                    pomoWorkSecs = workSecs,
                    pomoShortBreakSecs = shortBreakSecs,
                    pomoLongBreakSecs = longBreakSecs,
                    pomoSessionsPerRound = sessionsPerRound
                )
            )
        }
    }

    fun startTimer(timer: Timer) {
        val startRemaining = if (timer.endTimeMillis > 0) {
            maxOf(0, ((timer.endTimeMillis - System.currentTimeMillis()) / 1000).toInt())
        } else {
            timer.remainingSeconds
        }
        if (startRemaining <= 0) return

        val endTime = System.currentTimeMillis() + startRemaining * 1000L
        viewModelScope.launch {
            repository.updateTimer(timer.copy(endTimeMillis = endTime))
            scheduleCompletionAlarm(timer.id, endTime)
            context.startService(TimerForegroundService.startIntent(context, endTime, timer.label))
            refreshAllWidgets(context)
        }
    }

    fun pauseTimer(timer: Timer) {
        val remaining = if (timer.endTimeMillis > 0) {
            maxOf(0, ((timer.endTimeMillis - System.currentTimeMillis()) / 1000).toInt())
        } else {
            timer.remainingSeconds
        }
        viewModelScope.launch {
            cancelCompletionAlarm(timer.id)
            repository.updateTimer(timer.copy(remainingSeconds = remaining, endTimeMillis = 0))
            checkAndStopService()
            refreshAllWidgets(context)
        }
    }

    fun resetTimer(timer: Timer) {
        viewModelScope.launch {
            cancelCompletionAlarm(timer.id)
            repository.updateTimer(timer.copy(remainingSeconds = timer.durationSeconds, endTimeMillis = 0))
            checkAndStopService()
            refreshAllWidgets(context)
        }
    }

    fun deleteTimer(timer: Timer) {
        viewModelScope.launch {
            cancelCompletionAlarm(timer.id)
            repository.deleteTimer(timer)
            checkAndStopService()
            refreshAllWidgets(context)
        }
    }

    private fun scheduleCompletionAlarm(timerId: Long, endTimeMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = completionPendingIntent(timerId)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pi)
    }

    private fun cancelCompletionAlarm(timerId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(completionPendingIntent(timerId))
    }

    private fun completionPendingIntent(timerId: Long): PendingIntent {
        val intent = Intent(context, TimerCompletionReceiver::class.java).apply {
            putExtra(TimerCompletionReceiver.EXTRA_TIMER_ID, timerId)
        }
        return PendingIntent.getBroadcast(
            context, timerId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun checkAndStopService() {
        val anyRunning = timers.value.any { it.endTimeMillis > 0 }
        if (!anyRunning) {
            context.startService(TimerForegroundService.stopIntent(context))
        }
    }

    companion object {
        @Suppress("unused")
        private const val TIMER_NOTIF_ID_BASE = 900_000
    }
}
