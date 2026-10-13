package com.apagon.rhythm.data.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * Where this computer's sync server is listening right now (2026-10-13). It used to be a value
 * computed once at launch, so a Tailscale that came up after Bori did left the server on loopback,
 * unreachable from the phone, until Bori was quit and restarted. The desktop's SyncAddressMonitor
 * now re-checks every few seconds and rebinds, and the Sync screen follows this state.
 */
sealed interface SyncListenState {
    data object Starting : SyncListenState

    /** [viaTailscale] false means loopback only: nothing else can reach this computer. */
    data class Listening(val host: String, val port: Int, val viaTailscale: Boolean) : SyncListenState {
        /** Plain "host:port", what the pairing QR code encodes. */
        val address: String get() = "$host:$port"
    }

    data class Failed(val reason: String) : SyncListenState
}

/** commonMain view of the desktop's SyncAddressMonitor. */
interface SyncAddressSource {
    val state: StateFlow<SyncListenState>
}
