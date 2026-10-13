package com.apagon.rhythm.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.ServerSocket

/**
 * Owns the sync server's bind address. Every [CHECK_MS] it asks [findTailscaleAddress] where
 * Tailscale is; when that changes (Tailscale started after Bori, stopped, or got a new address) it
 * stops the server and starts it on the new host. Still never 0.0.0.0: the Tailscale address, or
 * 127.0.0.1 when there is none.
 *
 * Binding is checked up front with a plain ServerSocket, so a port another program holds becomes
 * [SyncListenState.Failed] (shown in Settings → Data Management → Sync with Phone & Computers, and retried) instead of an exception at launch.
 */
class SyncAddressMonitor(
    private val server: SyncServer,
    private val port: Int
) : SyncAddressSource {
    private val _state = MutableStateFlow<SyncListenState>(SyncListenState.Starting)
    override val state: StateFlow<SyncListenState> = _state.asStateFlow()

    private var boundHost: String? = null

    fun start(scope: CoroutineScope) {
        check()
        scope.launch(Dispatchers.IO) {
            while (true) {
                delay(CHECK_MS)
                check()
            }
        }
    }

    @Synchronized
    private fun check() {
        val tailscale = findTailscaleAddress()
        val host = tailscale ?: "127.0.0.1"
        if (host == boundHost && _state.value is SyncListenState.Listening) return
        if (boundHost != null) runCatching { server.stop() }
        boundHost = null
        _state.value = runCatching {
            ServerSocket().use { it.bind(InetSocketAddress(host, port)) }
            server.start(bindHost = host, port = port)
            boundHost = host
            SyncListenState.Listening(host, port, viaTailscale = tailscale != null)
        }.getOrElse {
            SyncListenState.Failed("Couldn't listen on $host:$port — another program may be using port $port.")
        }
    }

    private companion object {
        const val CHECK_MS = 15_000L
    }
}
