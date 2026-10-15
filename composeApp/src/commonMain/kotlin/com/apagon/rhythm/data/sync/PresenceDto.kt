package com.apagon.rhythm.data.sync

import kotlinx.serialization.Serializable

/**
 * The presence socket (2026-10-15): a phone can't be reached by a computer, so while Bori is open
 * on the phone it keeps one websocket open to each of its computers at [PRESENCE_PATH]. Over it the
 * computer can ask "sync now", and the phone runs its ordinary `/sync` to that computer and answers.
 * Identical file in both apps.
 *
 *  - phone → computer: `hello` (deviceId, deviceName, deviceOs), then `sync-result` per request.
 *  - computer → phone: `sync-request` (requestId), or `refused` (this phone is switched off here).
 */
@Serializable
data class PresenceMessage(
    val type: String,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val deviceOs: String? = null,
    val requestId: String? = null,
    val ok: Boolean? = null,
    /** Items the phone received from the computer in this sync. */
    val received: Int? = null,
    val error: String? = null
)

const val PRESENCE_PATH = "/presence"
const val PRESENCE_HELLO = "hello"
const val PRESENCE_SYNC_REQUEST = "sync-request"
const val PRESENCE_SYNC_RESULT = "sync-result"
const val PRESENCE_REFUSED = "refused"
