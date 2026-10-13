package com.apagon.rhythm.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * "Sync automatically" — the same choices as Android's `AutoSync.CHOICES` (2026-10-13), in
 * minutes; 0 = only when asked.
 */
object AutoSyncChoices {
    val CHOICES: List<Pair<Int, String>> = listOf(
        0 to "Off — only when I tap Sync",
        15 to "Every 15 minutes",
        30 to "Every 30 minutes",
        60 to "Every hour",
        360 to "Every 6 hours",
        720 to "Every 12 hours",
        1440 to "Once a day"
    )

    fun label(minutes: Int): String = CHOICES.firstOrNull { it.first == minutes }?.second ?: "Every $minutes minutes"
}

/**
 * Syncs with every switched-on outgoing computer every [minutes] while Bori runs (it lives in the
 * tray, so that's most of the time). Phones can't be reached from here — they connect *to* this
 * computer on their own schedule, set on the phone.
 *
 * A 30 s tick rather than one long delay, so a changed choice takes effect without a restart and a
 * computer that wakes from sleep catches up straight away instead of waiting out a whole period.
 * Failures are quiet: an unreachable computer is just tried again next period.
 */
class AutoSyncRunner(
    private val coordinator: SyncCoordinator,
    private val preferences: SyncPreferences
) {
    private val _minutes = MutableStateFlow<Int?>(null)
    val minutes: StateFlow<Int?> = _minutes.asStateFlow()

    private var lastRun = 0L

    fun start(scope: CoroutineScope) {
        scope.launch {
            _minutes.value = preferences.getAutoSyncMinutes()
            lastRun = System.currentTimeMillis() // first run one period after launch, like the phone
            while (true) {
                delay(30_000L)
                val m = _minutes.value ?: 0
                if (m <= 0) continue
                if (System.currentTimeMillis() - lastRun < m * 60_000L) continue
                lastRun = System.currentTimeMillis()
                for (d in preferences.devices().filter { it.outgoing && it.enabled }) {
                    runCatching { coordinator.syncWith(d.key) }
                }
            }
        }
    }

    suspend fun setMinutes(minutes: Int) {
        val m = minutes.coerceAtLeast(0)
        preferences.setAutoSyncMinutes(m)
        if (m != _minutes.value) lastRun = System.currentTimeMillis()
        _minutes.value = m
    }
}
