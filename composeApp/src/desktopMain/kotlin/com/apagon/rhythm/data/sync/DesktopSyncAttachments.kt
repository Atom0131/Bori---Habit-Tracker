package com.apagon.rhythm.data.sync

import com.apagon.rhythm.data.backup.BackupFolder
import com.apagon.rhythm.data.db.JournalDao
import com.apagon.rhythm.data.db.NotesDao
import com.apagon.rhythm.platform.AppHome
import com.apagon.rhythm.platform.PictureFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Base64

/**
 * See [SyncAttachments]. What each peer has been sent is kept in `pictures_sent.json`, by picture
 * name, so a picture goes to each device once, whatever the watermarks do; a picture that arrived
 * from a peer counts as one that peer has. Pictures that didn't fit in one sync go in the next.
 */
class DesktopSyncAttachments(
    private val notesDao: NotesDao,
    private val journalDao: JournalDao
) : SyncAttachments {
    private val file get() = File(AppHome.dir, "pictures_sent.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()

    @Serializable
    private data class Store(val sent: Map<String, Set<String>> = emptyMap(), val answeredBy: Map<String, String> = emptyMap())

    private fun read(): Store = runCatching { json.decodeFromString(Store.serializer(), file.readText()) }.getOrDefault(Store())
    private fun write(store: Store) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(json.encodeToString(Store.serializer(), store))
        tmp.renameTo(file)
    }

    private fun key(peer: String) = peer.trim().lowercase()

    /** name → kind for every picture a note or journal entry here refers to, trashed ones included. */
    private suspend fun referenced(): Map<String, Pair<String, String>> {
        val out = LinkedHashMap<String, Pair<String, String>>()
        notesDao.getAllNotesForBackup().forEach { note ->
            PictureFiles.noteImagePaths(note.content).forEach { path ->
                PictureFiles.nameOf(path)?.let { out.putIfAbsent(it, PictureDto.NOTE to path) }
            }
        }
        journalDao.getAllForBackup().forEach { entry ->
            PictureFiles.journalPhotoPaths(entry.photoUris).forEach { path ->
                PictureFiles.nameOf(path)?.let { out.putIfAbsent(it, PictureDto.JOURNAL to path) }
            }
        }
        return out
    }

    override suspend fun outgoingPictures(peer: String): List<PictureDto> = withContext(Dispatchers.IO) {
        val sent = lock.withLock { read().sent[key(peer)].orEmpty() }
        val out = mutableListOf<PictureDto>()
        var budget = MAX_BYTES_PER_SYNC
        for ((name, ref) in referenced()) {
            if (name in sent) continue
            val f = PictureFiles.resolve(ref.second).takeIf { it.isFile } ?: continue
            val bytes = runCatching { PictureFiles.bytesForTransfer(f) }.getOrNull() ?: continue
            // One picture bigger than the whole allowance still goes, alone, so it can't stall.
            if (bytes.size > budget && out.isNotEmpty()) continue
            out += PictureDto(name, ref.first, Base64.getEncoder().encodeToString(bytes))
            budget -= bytes.size
            if (budget <= 0) break
        }
        out
    }

    override suspend fun receivePictures(peer: String, peerDeviceId: String?, pictures: List<PictureDto>) {
        withContext(Dispatchers.IO) {
            for (p in pictures) {
                runCatching { PictureFiles.saveIncoming(p.name, p.kind, Base64.getDecoder().decode(p.data)) }
            }
            lock.withLock {
                var store = read()
                val k = key(peer)
                if (!peerDeviceId.isNullOrBlank() && store.answeredBy[k] != null && store.answeredBy[k] != peerDeviceId) {
                    store = store.copy(sent = store.sent - k) // a different device answers here now
                }
                if (!peerDeviceId.isNullOrBlank()) store = store.copy(answeredBy = store.answeredBy + (k to peerDeviceId))
                if (pictures.isNotEmpty()) {
                    store = store.copy(sent = store.sent + (k to (store.sent[k].orEmpty() + pictures.map { it.name })))
                }
                write(store)
            }
        }
    }

    override suspend fun markSent(peer: String, pictures: List<PictureDto>) {
        if (pictures.isEmpty()) return
        withContext(Dispatchers.IO) {
            lock.withLock {
                val store = read()
                val k = key(peer)
                write(store.copy(sent = store.sent + (k to (store.sent[k].orEmpty() + pictures.map { it.name }))))
            }
        }
    }

    override suspend fun receivePhoneBackup(fromName: String?, backup: BackupFileDto) {
        if (!BackupFolder.isAutoBackupName(backup.name)) return
        withContext(Dispatchers.IO) {
            runCatching {
                val folder = BackupFolder.phoneFolder(fromName).apply { mkdirs() }
                val dest = File(folder, backup.name)
                val tmp = File(folder, "${backup.name}.part")
                tmp.writeBytes(Base64.getDecoder().decode(backup.data))
                if (dest.exists()) dest.delete()
                tmp.renameTo(dest)
                BackupFolder.prune(folder)
            }
        }
    }

    private companion object {
        /** About 2 MB of pictures per sync (a handful of photos), so a sync over a phone's connection finishes in time; the rest follow. */
        const val MAX_BYTES_PER_SYNC = 2_000_000
    }
}
