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
    /**
     * The outgoing watermark **per peer** (2026-10-09): the phone's `deviceId` when this desktop is
     * the server, the address when it is the client. It used to be one value for every peer, so a
     * new or reinstalled phone only ever received what changed after the last sync with anyone.
     * A peer never seen before gets 0, i.e. everything.
     */
    suspend fun getLastSyncedAt(peer: String): Long
    suspend fun setLastSyncedAt(peer: String, timestamp: Long)
    /** Forgets every peer's watermark, so the next sync with each one is a full exchange. */
    suspend fun clearLastSyncedAt()
    suspend fun getPeerAddress(): String?
    suspend fun setPeerAddress(address: String?)
}
