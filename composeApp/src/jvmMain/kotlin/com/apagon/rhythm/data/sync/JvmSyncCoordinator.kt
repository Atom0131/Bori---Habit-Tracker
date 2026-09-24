package com.apagon.rhythm.data.sync

/** Bridges the commonMain-safe SyncCoordinator interface to the real Ktor-based SyncClient (jvmMain only). */
class JvmSyncCoordinator(
    private val syncClient: SyncClient
) : SyncCoordinator {
    override suspend fun syncWith(peerAddress: String): SyncResult {
        val (host, port) = parsePeerAddress(peerAddress)
        return syncClient.syncWith(host, port)
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
