package com.apagon.rhythm.data.sync

/**
 * commonMain-safe entry point to trigger a sync round from a ViewModel.
 * Exists so DesktopHabitViewModel (commonMain, deliberately shareable with
 * Android later) never has to reference a jvmMain-only Ktor type directly —
 * the real network exchange lives behind this in JvmSyncCoordinator.
 */
interface SyncCoordinator {
    suspend fun syncWith(peerAddress: String): SyncResult
    /** Whether something is listening on [peerAddress]'s sync port right now (Online/Offline). */
    suspend fun isReachable(peerAddress: String): Boolean = false

    /** Phones (by device id) with Bori open and connected to this computer right now; see [PresenceMessage]. */
    val connectedPhones: kotlinx.coroutines.flow.StateFlow<Set<String>>
        get() = kotlinx.coroutines.flow.MutableStateFlow(emptySet())

    /** Asks a connected phone to sync with this computer now, and waits for its answer. */
    suspend fun requestPhoneSync(deviceId: String): PhoneSyncOutcome = PhoneSyncOutcome.NotConnected
}

sealed interface PhoneSyncOutcome {
    data class Done(val received: Int) : PhoneSyncOutcome
    data class Failed(val message: String) : PhoneSyncOutcome
    /** Bori isn't open on the phone (or it's a version without the presence socket). */
    data object NotConnected : PhoneSyncOutcome
    data object TimedOut : PhoneSyncOutcome
}
