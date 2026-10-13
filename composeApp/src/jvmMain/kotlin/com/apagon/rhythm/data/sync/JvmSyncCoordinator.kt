package com.apagon.rhythm.data.sync

import kotlinx.coroutines.sync.withLock

/** Bridges the commonMain-safe SyncCoordinator interface to the real Ktor-based SyncClient (jvmMain only). */
class JvmSyncCoordinator(
    private val syncClient: SyncClient
) : SyncCoordinator {
    /** One outgoing sync at a time: the automatic sync (AutoSyncRunner) and a click must never interleave watermarks. */
    private val lock = kotlinx.coroutines.sync.Mutex()

    override suspend fun syncWith(peerAddress: String): SyncResult {
        val (host, port) = parsePeerAddress(peerAddress)
        return lock.withLock { syncClient.syncWith(host, port) }
    }

    override suspend fun isReachable(peerAddress: String): Boolean =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val (host, port) = parsePeerAddress(peerAddress)
            runCatching {
                java.net.Socket().use { it.connect(java.net.InetSocketAddress(host, port), 1_500) }
            }.isSuccess
        }

    private fun parsePeerAddress(address: String): Pair<String, Int> {
        val trimmed = address.trim()
        val idx = trimmed.lastIndexOf(':')
        return if (idx > 0) {
            val host = trimmed.substring(0, idx)
            val port = trimmed.substring(idx + 1).toIntOrNull() ?: DEFAULT_SYNC_PORT
            host to port
        } else {
            trimmed to DEFAULT_SYNC_PORT
        }
    }
}

const val DEFAULT_SYNC_PORT = 47890
