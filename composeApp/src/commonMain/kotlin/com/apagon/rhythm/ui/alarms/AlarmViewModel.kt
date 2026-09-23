package com.apagon.rhythm.ui.alarms

import com.apagon.rhythm.platform.PurchaseLauncher

import com.apagon.rhythm.platform.ReminderScheduling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.repository.AlarmRepository
import com.apagon.rhythm.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class AlarmViewModel constructor(
    private val repository: AlarmRepository,
    private val themePreferences: ThemePreferences,
    private val purchaseLauncher: PurchaseLauncher,
    private val scheduler: ReminderScheduling
) : ViewModel() {

    val isPro: StateFlow<Boolean> = themePreferences.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun startBillingFlow(productId: String = PurchaseLauncher.PRO_MONTHLY_ID) {
        purchaseLauncher.launchPurchase(productId)
    }

    val alarms = repository.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAlarm(label: String, hour: Int, minute: Int, repeatDays: Int, soundUri: String, vibrationPatternId: String = "default") {
        viewModelScope.launch {
            val newAlarm = Alarm(
                label = label,
                hour = hour,
                minute = minute,
                repeatDaysMask = repeatDays,
                soundUri = soundUri,
                vibrationPatternId = vibrationPatternId,
                isEnabled = true
            )
            val id = repository.addAlarm(newAlarm)
            scheduler.scheduleAlarm(newAlarm.copy(id = id)
            )
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
            if (alarm.isEnabled) {
                scheduler.scheduleAlarm(alarm)
            } else {
                scheduler.cancelAlarm(alarm.id)
            }
        }
    }

    fun toggleEnabled(alarm: Alarm) {
        updateAlarm(alarm.copy(isEnabled = !alarm.isEnabled))
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancelAlarm(alarm.id)
            repository.deleteAlarm(alarm)
        }
    }
}
