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
    private val deviceId: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** One WebSocket round trip: send our outgoing batch, apply the peer's reply, then advance our own lastSyncedAt. */
    suspend fun syncWith(peerHost: String, port: Int): SyncResult {
        val peer = "$peerHost:$port"
        val startedAt = System.currentTimeMillis()
        val outgoingBatch = syncEngine.buildOutgoingBatch(deviceId, peer)
        val client = HttpClient(CIO) { install(WebSockets) }
        val result = try {
            var applied: SyncResult? = null
            client.webSocket(host = peerHost, port = port, path = "/sync") {
                send(Frame.Text(json.encodeToString(SyncBatch.serializer(), outgoingBatch)))
                val responseFrame = incoming.receive() as Frame.Text
                val responseBatch = json.decodeFromString(SyncBatch.serializer(), responseFrame.readText())
                applied = syncEngine.applyIncomingBatch(responseBatch)
            }
            applied ?: SyncResult(0, 0, 0, 0)
        } finally {
            client.close()
        }
        syncEngine.markSynced(peer, startedAt)
        return result
    }
}
