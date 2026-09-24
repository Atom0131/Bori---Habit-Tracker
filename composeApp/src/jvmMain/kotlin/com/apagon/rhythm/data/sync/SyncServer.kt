package com.apagon.rhythm.data.sync

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json

/**
 * Binds to a single explicit interface — never 0.0.0.0 — matching this
 * project's Tailscale-first security stance: reachable only from loopback
 * (Stage 4b's two-local-instance test) or the device's own tailnet IP
 * (Stage 4c), never every interface on the box.
 */
class SyncServer(
    private val syncEngine: SyncEngine,
    private val deviceId: String
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var server: EmbeddedServer<*, *>? = null

    fun start(bindHost: String, port: Int) {
        server = embeddedServer(CIO, host = bindHost, port = port) {
            install(WebSockets)
            routing {
                webSocket("/sync") {
                    val incomingFrame = incoming.receive() as? Frame.Text ?: return@webSocket
                    val incomingBatch = json.decodeFromString(SyncBatch.serializer(), incomingFrame.readText())
                    syncEngine.applyIncomingBatch(incomingBatch)
                    val outgoingBatch = syncEngine.buildOutgoingBatch(deviceId)
                    send(Frame.Text(json.encodeToString(SyncBatch.serializer(), outgoingBatch)))
                    syncEngine.markSynced()
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 200, timeoutMillis = 1000)
        server = null
    }
}
