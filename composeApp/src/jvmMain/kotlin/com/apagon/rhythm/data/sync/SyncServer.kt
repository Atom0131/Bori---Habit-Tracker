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
    private val deviceId: String,
    private val syncPreferences: SyncPreferences,
    private val presence: PresenceRegistry
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var server: EmbeddedServer<*, *>? = null
    /** host:port this server listens on, so a phone's list of computers can't introduce us to ourselves. */
    private var ownAddress: String = ""

    fun start(bindHost: String, port: Int) {
        ownAddress = "$bindHost:$port"
        server = embeddedServer(CIO, host = bindHost, port = port) {
            // Pings keep the phones' presence sockets alive across idle minutes and notice a phone
            // that vanished (screen off, Tailscale dropped) within half a minute.
            install(WebSockets) {
                pingPeriodMillis = 15_000L
                timeoutMillis = 30_000L
            }
            routing {
                webSocket(PRESENCE_PATH) {
                    val hello = (incoming.receive() as? Frame.Text)?.readText()
                        ?.let { runCatching { json.decodeFromString(PresenceMessage.serializer(), it) }.getOrNull() }
                        ?.takeIf { it.type == PRESENCE_HELLO && !it.deviceId.isNullOrBlank() }
                        ?: return@webSocket
                    val phone = hello.deviceId!!
                    syncPreferences.recordIncoming(phone, hello.deviceName, hello.deviceOs)
                    if (syncPreferences.devices().any { !it.outgoing && it.key.equals(phone, ignoreCase = true) && !it.enabled }) {
                        send(Frame.Text(json.encodeToString(PresenceMessage.serializer(),
                            PresenceMessage(type = PRESENCE_REFUSED, error = "${localDeviceName()} has sync with this device turned off."))))
                        return@webSocket
                    }
                    presence.register(phone, this)
                    try {
                        for (frame in incoming) {
                            val text = (frame as? Frame.Text)?.readText() ?: continue
                            val message = runCatching { json.decodeFromString(PresenceMessage.serializer(), text) }.getOrNull() ?: continue
                            if (message.type == PRESENCE_SYNC_RESULT) presence.complete(message)
                        }
                    } finally {
                        presence.unregister(phone, this)
                    }
                }

                webSocket("/sync") {
                    val incomingFrame = incoming.receive() as? Frame.Text ?: return@webSocket
                    val incomingBatch = json.decodeFromString(SyncBatch.serializer(), incomingFrame.readText())
                    // Watermark per phone, keyed by the deviceId every batch carries — see
                    // SyncPreferences.getLastSyncedAt.
                    val peer = incomingBatch.deviceId
                    syncPreferences.recordIncoming(peer, incomingBatch.deviceName, incomingBatch.deviceOs)
                    // Turned off in this computer's device list: refuse, applying nothing. A device
                    // that sends no name predates refusals and would read a refusal as an empty
                    // successful sync (and move its watermark past changes it never sent), so it
                    // gets a closed connection instead, which it reports as a failure.
                    if (syncPreferences.devices().any { !it.outgoing && it.key.equals(peer, ignoreCase = true) && !it.enabled }) {
                        if (incomingBatch.deviceName != null) {
                            val name = localDeviceName()
                            val refusal = SyncBatch(
                                deviceId = deviceId,
                                habits = emptyList(),
                                completions = emptyList(),
                                deviceName = name,
                                deviceOs = localDeviceOs(),
                                refused = "$name has sync with this device turned off."
                            )
                            send(Frame.Text(json.encodeToString(SyncBatch.serializer(), refusal)))
                        }
                        return@webSocket
                    }
                    // Rows the old go-between rule stranded only go out in a full sync (once per install).
                    syncPreferences.resetWatermarksOnceForRelayFix()
                    incomingBatch.autoSyncMinutes?.let { syncPreferences.setPhoneAutoSyncMinutes(peer, it) }
                    for (computer in incomingBatch.knownComputers) {
                        if (computer.deviceId.equals(deviceId, ignoreCase = true)) continue
                        if (computer.address.trim().equals(ownAddress, ignoreCase = true)) continue
                        syncPreferences.addIntroducedComputer(computer.address, computer.name, computer.os)
                    }
                    val startedAt = System.currentTimeMillis()
                    val applied = syncEngine.applyIncomingBatch(incomingBatch)
                    val outgoingBatch = syncEngine.buildOutgoingBatch(deviceId, peer)
                        .copy(deviceName = localDeviceName(), deviceOs = localDeviceOs())
                    send(Frame.Text(json.encodeToString(SyncBatch.serializer(), outgoingBatch)))
                    syncEngine.markSynced(peer, startedAt)
                    // Pass on to every other device what this one just sent.
                    applied.oldestAppliedAt?.let { syncPreferences.lowerOtherWatermarks(peer, it) }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 200, timeoutMillis = 1000)
        server = null
    }
}
