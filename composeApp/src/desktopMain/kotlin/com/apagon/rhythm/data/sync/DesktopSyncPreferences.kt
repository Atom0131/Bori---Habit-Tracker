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
    val lastSyncedAt: Long = 0L,
    val peerAddress: String? = null
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

    override suspend fun getLastSyncedAt(): Long = read().lastSyncedAt

    override suspend fun setLastSyncedAt(timestamp: Long) = mutex.withLock {
        write(read().copy(lastSyncedAt = timestamp))
    }

    override suspend fun getPeerAddress(): String? = read().peerAddress

    override suspend fun setPeerAddress(address: String?) = mutex.withLock {
        write(read().copy(peerAddress = address))
    }
}
