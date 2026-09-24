package com.apagon.rhythm.data.sync

/**
 * Small per-device sync bookkeeping: the last successful sync's timestamp
 * (the "last known agreement point" a batch's since-filter is built from)
 * and the peer's address, entered manually since Android's Tailscale app
 * has no CLI to auto-discover it from. Deliberately not routed through
 * [com.apagon.rhythm.data.preferences.ThemePreferences] — that class has no
 * desktop wiring at all today, and two small values don't justify building
 * real DataStore plumbing for desktop from scratch. Android actual:
 * AndroidSyncPreferences (real DataStore, mirroring AppModule.kt's existing
 * precedent). Desktop actual: DesktopSyncPreferences (a hand-rolled JSON
 * file, mirroring DesktopHabitDatabase.kt's ~/.rhythm/ convention).
 */
interface SyncPreferences {
    suspend fun getLastSyncedAt(): Long
    suspend fun setLastSyncedAt(timestamp: Long)
    suspend fun getPeerAddress(): String?
    suspend fun setPeerAddress(address: String?)
}
