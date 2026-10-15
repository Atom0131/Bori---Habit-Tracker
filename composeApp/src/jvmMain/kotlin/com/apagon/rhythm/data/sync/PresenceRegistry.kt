package com.apagon.rhythm.data.sync

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The phones connected to this computer's presence socket right now ([PRESENCE_PATH], served by
 * [SyncServer]), and the sync requests waiting on their answers. Shared by the server (which adds
 * and removes sessions) and [JvmSyncCoordinator] (which sends requests).
 */
class PresenceRegistry {
    private val json = Json { ignoreUnknownKeys = true }
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<PresenceMessage>>()

    private val _connected = MutableStateFlow<Set<String>>(emptySet())
    val connected: StateFlow<Set<String>> = _connected.asStateFlow()

    private fun key(deviceId: String) = deviceId.trim().lowercase()

    fun register(deviceId: String, session: WebSocketSession) {
        sessions[key(deviceId)] = session
        _connected.value = sessions.keys.toSet()
    }

    /** Only removes [session] if it's still the current one, so a quick reconnect isn't dropped. */
    fun unregister(deviceId: String, session: WebSocketSession) {
        sessions.remove(key(deviceId), session)
        _connected.value = sessions.keys.toSet()
    }

    fun complete(message: PresenceMessage) {
        message.requestId?.let { pending.remove(it)?.complete(message) }
    }

    /** Sends a sync request and waits up to [timeoutMs] for the phone's sync to finish. */
    suspend fun requestSync(deviceId: String, timeoutMs: Long = 60_000L): PhoneSyncOutcome {
        val session = sessions[key(deviceId)] ?: return PhoneSyncOutcome.NotConnected
        val requestId = UUID.randomUUID().toString()
        val answer = CompletableDeferred<PresenceMessage>()
        pending[requestId] = answer
        val sent = runCatching {
            session.send(Frame.Text(json.encodeToString(PresenceMessage.serializer(),
                PresenceMessage(type = PRESENCE_SYNC_REQUEST, requestId = requestId))))
        }.isSuccess
        if (!sent) {
            pending.remove(requestId)
            return PhoneSyncOutcome.NotConnected
        }
        val reply = withTimeoutOrNull(timeoutMs) { answer.await() }
        pending.remove(requestId)
        return when {
            reply == null -> PhoneSyncOutcome.TimedOut
            reply.ok == true -> PhoneSyncOutcome.Done(reply.received ?: 0)
            else -> PhoneSyncOutcome.Failed(reply.error ?: "The phone couldn't sync")
        }
    }
}
