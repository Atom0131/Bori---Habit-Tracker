package com.apagon.rhythm.platform

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class FiredAlertKind { ALARM, REMINDER, TIMER }

data class FiredAlert(
    val kind: FiredAlertKind,
    val sourceId: Long,
    val title: String,
    val subtitle: String = "",
    val isPomo: Boolean = false
)

/**
 * In-process event bus for alarm/timer/reminder firings (Stage 12) — replaces
 * Android's AlarmManager + BroadcastReceiver + full-screen-Activity chain,
 * which has no desktop equivalent. Desktop's DesktopAlarmClockService (a
 * polling scheduler, desktopMain) raises alerts here; a single always-on-top
 * alert window in main.kt collects and displays whichever fires. commonMain
 * (not desktopMain-only) so a future iOS actual could raise through the same
 * bus via UNUserNotificationCenter without a new abstraction.
 *
 * No persistence beyond the SharedFlow buffer: like AlarmManager itself, an
 * alert only has an effect while something is actually running to receive
 * it — there's no boot-time replay on desktop, same accepted limitation the
 * roadmap already notes for the whole scheduler.
 */
class AlertCenter {
    private val _alerts = MutableSharedFlow<FiredAlert>(extraBufferCapacity = 8)
    val alerts: SharedFlow<FiredAlert> = _alerts

    suspend fun raise(alert: FiredAlert) {
        _alerts.emit(alert)
    }
}
