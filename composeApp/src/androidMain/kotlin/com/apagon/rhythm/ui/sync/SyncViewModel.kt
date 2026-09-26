package com.apagon.rhythm.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.sync.SyncCoordinator
import com.apagon.rhythm.data.sync.SyncPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Stage 13: Android is always the sync CLIENT — the desktop app always runs
// the server (see plan_2026-09-25_stage13_tailscale_pairing.md's design
// decision on sync direction; running a reliably-reachable server inside the
// Android process is a much bigger lift than a client that connects out on
// demand). Deliberately its own small ViewModel rather than a new dependency
// on HabitListViewModel (already a 10-param constructor driving Android's
// real, most-used screen) — same shape as DesktopHabitViewModel's sync
// slice, kept separate so this stays low-risk to add.
class SyncViewModel(
    private val syncCoordinator: SyncCoordinator,
    private val syncPreferences: SyncPreferences
) : ViewModel() {

    private val _peerAddress = MutableStateFlow("")
    val peerAddress: StateFlow<String> = _peerAddress.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow(0L)
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    init {
        viewModelScope.launch {
            _peerAddress.value = syncPreferences.getPeerAddress() ?: ""
            _lastSyncedAt.value = syncPreferences.getLastSyncedAt()
        }
    }

    fun updatePeerAddress(address: String) {
        _peerAddress.value = address
    }

    fun syncNow() {
        val address = _peerAddress.value.trim()
        if (address.isEmpty()) {
            _syncStatus.value = "Enter the desktop's Tailscale address first"
            return
        }
        viewModelScope.launch {
            _syncStatus.value = "Syncing…"
            runCatching {
                syncPreferences.setPeerAddress(address)
                syncCoordinator.syncWith(address)
            }.onSuccess { result ->
                _lastSyncedAt.value = syncPreferences.getLastSyncedAt()
                _syncStatus.value =
                    "Synced — habits +${result.habitsInserted}/${result.habitsUpdated}, " +
                        "completions +${result.completionsInserted}/${result.completionsUpdated}"
            }.onFailure { e ->
                _syncStatus.value = "Sync failed: ${e.message}"
            }
        }
    }
}
