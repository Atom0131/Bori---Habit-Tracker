package com.apagon.rhythm.data.sync

import com.apagon.rhythm.core.json.JSONObject
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.platform.ProfileImageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Settings a peer sent to a device that has not joined yet, waiting for the user's answer. */
@Serializable
data class PendingPreferences(
    val peerName: String? = null,
    val preferences: PreferencesDto
)

/** Where [PreferencesJoin] keeps what is waiting, so it survives the app closing. null = nothing. */
interface PendingPreferencesStore {
    suspend fun read(): String?
    suspend fun write(text: String?)
}

/**
 * **A new device asks before its settings meet everyone else's (2026-10-23)** — the phone's
 * PreferencesJoin, mirrored. Its first sync with a device that has already joined neither applies
 * the incoming settings nor sends its own over them: they wait here until the user picks
 * - [useExisting] — take the settings the other devices already share (the usual answer), or
 * - [keepThisDevice] — make this device's settings the newest, so they go to every other device on
 *   the next sync.
 *
 * Either way the Borikén unlock is kept on every device (see ThemePreferences.importPreferencesForSync),
 * and this device counts as joined from then on, so later edits sync by plain last-write-wins.
 */
class PreferencesJoin(
    private val store: PendingPreferencesStore,
    private val themePreferences: ThemePreferences,
    private val syncPreferences: SyncPreferences,
    private val profileImageStore: ProfileImageStore
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var loaded = false

    private val _pending = MutableStateFlow<PendingPreferences?>(null)
    val pending: StateFlow<PendingPreferences?> = _pending.asStateFlow()

    /** Reads what an earlier run left waiting. Called once from the window. */
    suspend fun load() = lock.withLock {
        if (loaded) return@withLock
        loaded = true
        _pending.value = store.read()?.let {
            runCatching { json.decodeFromString(PendingPreferences.serializer(), it) }.getOrNull()
        }
    }

    /** Called by SyncEngine. A later sync replaces what is waiting with the peer's newer settings. */
    suspend fun hold(preferences: PreferencesDto, peerName: String?) = lock.withLock {
        val held = PendingPreferences(peerName, preferences)
        store.write(json.encodeToString(PendingPreferences.serializer(), held))
        loaded = true
        _pending.value = held
    }

    suspend fun useExisting() = lock.withLock {
        val held = _pending.value ?: return@withLock
        held.preferences.profileImage?.let { profileImageStore.import(it) }
        themePreferences.importPreferencesForSync(JSONObject(held.preferences.values))
        themePreferences.setPreferencesUpdatedAtFromSync(held.preferences.updatedAt)
        finish()
    }

    suspend fun keepThisDevice() = lock.withLock {
        val held = _pending.value ?: return@withLock
        themePreferences.mergeUnlocksFromSync(JSONObject(held.preferences.values))
        themePreferences.restampForSync()
        finish()
    }

    private suspend fun finish() {
        syncPreferences.setPreferencesJoined()
        store.write(null)
        _pending.value = null
    }
}
