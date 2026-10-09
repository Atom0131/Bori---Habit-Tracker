package com.apagon.rhythm.data.sync

import com.apagon.rhythm.platform.AppHome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class SyncPrefsData(
    /** Pre-2026-10-09 single watermark. Still read so old files parse; no longer used. */
    val lastSyncedAt: Long = 0L,
    val peerAddress: String? = null,
    val peerWatermarks: Map<String, Long> = emptyMap()
)

// Desktop has no DataStore plumbing at all today (nothing constructs a
// DataStore instance for this target) — building that from scratch isn't
// worth it for two small values, so this is a plain JSON file, mirroring
// DesktopHabitDatabase.kt's existing ~/.rhythm/ convention exactly.
class DesktopSyncPreferences : SyncPreferences {
    private val file = File(AppHome.dir, "sync_prefs.json").apply {
        parentFile.mkdirs()
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private suspend fun read(): SyncPrefsData = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext SyncPrefsData()
        runCatching { json.decodeFromString<SyncPrefsData>(file.readText()) }.getOrDefault(SyncPrefsData())
    }

    private suspend fun write(data: SyncPrefsData) = withContext(Dispatchers.IO) {
        file.writeText(json.encodeToString(SyncPrefsData.serializer(), data))
    }

    override suspend fun getLastSyncedAt(peer: String): Long =
        read().peerWatermarks[peer.trim().lowercase()] ?: 0L

    override suspend fun setLastSyncedAt(peer: String, timestamp: Long) = mutex.withLock {
        val data = read()
        write(data.copy(peerWatermarks = data.peerWatermarks + (peer.trim().lowercase() to timestamp)))
    }

    override suspend fun clearLastSyncedAt() = mutex.withLock {
        write(read().copy(lastSyncedAt = 0L, peerWatermarks = emptyMap()))
    }

    override suspend fun getPeerAddress(): String? = read().peerAddress

    override suspend fun setPeerAddress(address: String?) = mutex.withLock {
        write(read().copy(peerAddress = address))
    }
}
