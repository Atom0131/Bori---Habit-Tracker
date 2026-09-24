package com.apagon.rhythm.data.sync

/**
 * commonMain-safe entry point to trigger a sync round from a ViewModel.
 * Exists so DesktopHabitViewModel (commonMain, deliberately shareable with
 * Android later) never has to reference a jvmMain-only Ktor type directly —
 * the real network exchange lives behind this in JvmSyncCoordinator.
 */
interface SyncCoordinator {
    suspend fun syncWith(peerAddress: String): SyncResult
}
