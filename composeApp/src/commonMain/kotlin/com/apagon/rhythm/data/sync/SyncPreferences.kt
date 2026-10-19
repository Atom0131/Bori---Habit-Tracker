package com.apagon.rhythm.data.sync

import kotlinx.serialization.Serializable

/**
 * A device in this computer's sync list (2026-10-13, mirroring Android's per-computer list).
 *  - [outgoing] true: a computer this one syncs *to* (Sync now), keyed by its address.
 *  - [outgoing] false: a phone or computer that syncs *in* to this one, keyed by its deviceId.
 * [enabled] false keeps it listed but stops the sync: an outgoing one is skipped by Sync now, an
 * incoming one is refused by the sync server. [name] is what the device calls itself.
 */
@Serializable
data class SyncDevice(
    val key: String,
    val outgoing: Boolean,
    val name: String? = null,
    val enabled: Boolean = true,
    /** "android", "windows", "linux", "macos"; null until the device has said (older versions). */
    val os: String? = null,
    /** A phone's own "Sync automatically" interval in minutes (0 = off), as it last said; null = unknown. */
    val autoSyncMinutes: Int? = null
) {
    val isPhone: Boolean get() = !outgoing && key.startsWith("android-")
}

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
    /** Every peer's watermark (the time of the last successful sync with it), for "last synced"
     *  displays. Keys are as in [getLastSyncedAt]. */
    suspend fun allLastSyncedAt(): Map<String, Long> = emptyMap()
    /** Forgets every peer's watermark, so the next sync with each one is a full exchange. */
    suspend fun clearLastSyncedAt()
    /**
     * **A computer can be the go-between too** (2026-10-19): what it receives from one device must go
     * on to the others. Rows keep the `updatedAt` they were made with and a peer is sent only rows
     * newer than its watermark, so a row that arrives after another peer's last sync, carrying an
     * older time, was never sent to that peer. Pulls every watermark except [exceptPeer]'s back to
     * just before [oldestAppliedAt], so the next sync with each re-sends it (newest still wins there).
     */
    suspend fun lowerOtherWatermarks(exceptPeer: String, oldestAppliedAt: Long) {}
    /** Once per install after that fix: forget every watermark, so stranded rows go out. True when it ran. */
    suspend fun resetWatermarksOnceForRelayFix(): Boolean = false
    /**
     * A phone told this computer about another computer it syncs with: list it (on) so the two can
     * sync each other directly, unless it is already listed or the user removed it before.
     * Returns true when it was added.
     */
    suspend fun addIntroducedComputer(address: String, name: String?, os: String?): Boolean = false
    /** Records a phone's own automatic-sync interval (see [SyncDevice.autoSyncMinutes]). */
    suspend fun setPhoneAutoSyncMinutes(deviceId: String, minutes: Int) {}
    /** Every listed device, incoming and outgoing. Keys match [getLastSyncedAt]'s. */
    suspend fun devices(): List<SyncDevice> = emptyList()
    /** A device just synced in: list it (on) if new, and keep its name current. */
    suspend fun recordIncoming(deviceId: String, name: String?, os: String? = null) {}
    /** Adds a computer to sync to (on), or turns an already-listed one back on. */
    suspend fun addComputer(address: String) {}
    suspend fun setDeviceName(key: String, outgoing: Boolean, name: String?, os: String? = null) {}
    suspend fun setDeviceEnabled(key: String, outgoing: Boolean, enabled: Boolean) {}
    suspend fun removeDevice(key: String, outgoing: Boolean) {}
    /** "Sync automatically", in minutes; 0 = only when asked. See [AutoSyncRunner]. */
    suspend fun getAutoSyncMinutes(): Int = 0
    suspend fun setAutoSyncMinutes(minutes: Int) {}
    suspend fun getPeerAddress(): String?
    suspend fun setPeerAddress(address: String?)
}
