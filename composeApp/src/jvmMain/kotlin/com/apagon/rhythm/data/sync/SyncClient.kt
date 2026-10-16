package com.apagon.rhythm.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json

class SyncClient(
    private val syncEngine: SyncEngine,
    private val deviceId: String,
    private val syncPreferences: SyncPreferences
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** One WebSocket round trip: send our outgoing batch, apply the peer's reply, then advance our own lastSyncedAt. */
    suspend fun syncWith(peerHost: String, port: Int): SyncResult {
        val peer = "$peerHost:$port"
        val startedAt = System.currentTimeMillis()
        val outgoingBatch = syncEngine.buildOutgoingBatch(deviceId, peer)
            .copy(deviceName = localDeviceName(), deviceOs = localDeviceOs())
        // A computer that's switched off never answers the connect. Give up after 3 s instead of
        // CIO's 5 s, as on Android, so automatic sync skips it quickly.
        val client = HttpClient(CIO) {
            engine { endpoint.connectTimeout = 3_000L }
            install(WebSockets)
        }
        val result = try {
            var applied: SyncResult? = null
            // Bounded, as on Android: a computer that accepts and never answers would hang "Syncing…".
            kotlinx.coroutines.withTimeout(30_000L) { client.webSocket(host = peerHost, port = port, path = "/sync") {
                send(Frame.Text(json.encodeToString(SyncBatch.serializer(), outgoingBatch)))
                val responseFrame = incoming.receive() as? Frame.Text
                    ?: throw IllegalStateException("The other computer closed the connection")
                val responseBatch = json.decodeFromString(SyncBatch.serializer(), responseFrame.readText())
                responseBatch.refused?.let { throw SyncRefusedException(it) }
                syncPreferences.setDeviceName(peer, outgoing = true, name = responseBatch.deviceName, os = responseBatch.deviceOs)
                applied = syncEngine.applyIncomingBatch(responseBatch)
            } }
            applied ?: SyncResult(0, 0, 0, 0)
        } finally {
            client.close()
        }
        syncEngine.markSynced(peer, startedAt)
        return result
    }
}

/** The other computer has sync with this one turned off; nothing was exchanged. */
class SyncRefusedException(message: String) : Exception(message)

/**
 * What this computer calls itself to other devices: Windows' computer name, else the Linux host
 * name, else whatever the JVM resolves.
 */
fun localDeviceName(): String =
    System.getenv("COMPUTERNAME")?.takeIf { it.isNotBlank() }
        ?: runCatching { java.io.File("/etc/hostname").readText().trim() }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: runCatching { java.net.InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: "Computer"

/** This computer's OS as the sync protocol names it: "windows", "macos" or "linux". */
fun localDeviceOs(): String {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        "windows" in os -> "windows"
        "mac" in os -> "macos"
        else -> "linux"
    }
}
