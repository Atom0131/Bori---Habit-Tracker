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
    val peerWatermarks: Map<String, Long> = emptyMap(),
    /** Null until first written; then the device list, replacing [peerAddress]. */
    val devices: List<SyncDevice>? = null,
    /** "Sync automatically", minutes; 0 = off. */
    val autoSyncMinutes: Int = 0
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

    override suspend fun allLastSyncedAt(): Map<String, Long> = read().peerWatermarks

    override suspend fun clearLastSyncedAt() = mutex.withLock {
        write(read().copy(lastSyncedAt = 0L, peerWatermarks = emptyMap()))
    }

    override suspend fun getPeerAddress(): String? = read().peerAddress

    /**
     * The stored list, or — before it's first written — the old single [SyncPrefsData.peerAddress]
     * as an outgoing computer plus every phone that has a watermark, so what was already paired
     * shows up without anyone re-pairing.
     */
    private fun listOf(data: SyncPrefsData): List<SyncDevice> {
        val stored = data.devices ?: buildList {
            data.peerAddress?.takeIf { it.isNotBlank() }?.let { add(SyncDevice(it.trim(), outgoing = true)) }
        }
        val known = stored.filter { !it.outgoing }.map { it.key.lowercase() }.toSet()
        val fromWatermarks = data.peerWatermarks.keys
            .filter { it.startsWith("android-") && it !in known }
            .map { SyncDevice(it, outgoing = false) }
        return stored + fromWatermarks
    }

    private suspend fun editDevices(block: (List<SyncDevice>) -> List<SyncDevice>) = mutex.withLock {
        val data = read()
        write(data.copy(devices = block(listOf(data))))
    }

    private fun SyncDevice.matches(key: String, outgoing: Boolean) =
        this.outgoing == outgoing && this.key.trim().lowercase() == key.trim().lowercase()

    override suspend fun devices(): List<SyncDevice> = listOf(read())

    override suspend fun recordIncoming(deviceId: String, name: String?, os: String?) {
        if (deviceId.isBlank()) return
        val n = name?.takeIf { it.isNotBlank() }
        val current = devices().firstOrNull { it.matches(deviceId, false) }
        if (current != null && (n == null || current.name == n) && (os == null || current.os == os)) return
        editDevices { list ->
            if (list.any { it.matches(deviceId, false) }) {
                list.map { if (it.matches(deviceId, false)) it.copy(name = n ?: it.name, os = os ?: it.os) else it }
            } else list + SyncDevice(deviceId, outgoing = false, name = n, os = os)
        }
    }

    override suspend fun addComputer(address: String) {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return
        editDevices { list ->
            if (list.any { it.matches(trimmed, true) }) list.map { if (it.matches(trimmed, true)) it.copy(enabled = true) else it }
            else list + SyncDevice(trimmed, outgoing = true)
        }
    }

    override suspend fun setDeviceName(key: String, outgoing: Boolean, name: String?, os: String?) {
        val n = name?.takeIf { it.isNotBlank() }
        if (n == null && os == null) return
        val current = devices().firstOrNull { it.matches(key, outgoing) } ?: return
        if ((n == null || current.name == n) && (os == null || current.os == os)) return
        editDevices { list ->
            list.map { if (it.matches(key, outgoing)) it.copy(name = n ?: it.name, os = os ?: it.os) else it }
        }
    }

    override suspend fun setDeviceEnabled(key: String, outgoing: Boolean, enabled: Boolean) =
        editDevices { list -> list.map { if (it.matches(key, outgoing)) it.copy(enabled = enabled) else it } }

    override suspend fun removeDevice(key: String, outgoing: Boolean) =
        editDevices { list -> list.filterNot { it.matches(key, outgoing) } }

    override suspend fun getAutoSyncMinutes(): Int = read().autoSyncMinutes

    override suspend fun setAutoSyncMinutes(minutes: Int) = mutex.withLock {
        write(read().copy(autoSyncMinutes = minutes.coerceAtLeast(0)))
    }

    override suspend fun setPeerAddress(address: String?) = mutex.withLock {
        write(read().copy(peerAddress = address))
    }
}
