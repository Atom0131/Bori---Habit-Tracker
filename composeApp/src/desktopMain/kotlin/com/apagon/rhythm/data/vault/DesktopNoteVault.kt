package com.apagon.rhythm.data.vault

import com.apagon.rhythm.data.db.NotesDao
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.platform.AppHome
import com.apagon.rhythm.ui.notes.BlockType
import com.apagon.rhythm.ui.notes.NoteBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date

/**
 * **Desktop Markdown vault: a folder of `.md` files kept in two-way sync with Bori's notes.**
 * Point it at an Obsidian vault and Obsidian, Bori and an AI agent writing files there (Coqui) all
 * see the same notes.
 *
 * - The folder is a notebook (named after it); each subfolder is a child notebook.
 * - A note is `<folder>/<Title>.md`. The file name is the title, as in Obsidian.
 * - The file format is [VaultMarkdown]: lines Bori didn't change stay exactly as they were.
 * - A file Bori has never written isn't touched just by opening the folder: nothing is added to it
 *   until the note is edited in Bori (on the computer, the phone or through the agent API).
 *
 * One [reconcile] pass does both directions, under a lock. It runs at start, when a file changes
 * ([WatchService]), when a note or notebook row changes (Room flows), and every minute as a backstop.
 * Each note's last synced state (file hash and the note's `updatedAt`) is kept in
 * `~/.bori/markdown_vault.json`, local to this computer, so a pass knows which side changed:
 * - only the file → the note is updated from it;
 * - only the note → the file is rewritten (renamed or moved too if the title or notebook changed);
 * - both → the newer side wins and the other version is saved under `~/.bori/vault_conflicts/`.
 * A deleted note's file goes to the vault's `.trash/` (Obsidian's own trash folder); a file deleted
 * outside Bori sends its note to Recently Deleted.
 */
class DesktopNoteVault(private val notesDao: NotesDao) : MarkdownVaultControl {

    @Serializable
    data class Entry(val path: String, val hash: String, val noteUpdatedAt: Long)

    @Serializable
    data class Index(
        val enabled: Boolean = false,
        val root: String? = null,
        /** Folder path relative to the root ("" = root) → notebook syncId. */
        val folders: Map<String, String> = emptyMap(),
        /** Note syncId → its file. */
        val notes: Map<String, Entry> = emptyMap(),
        val lastSyncAt: Long? = null
    )

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val indexFile = File(AppHome.dir, "markdown_vault.json")
    private val conflictsDir = File(AppHome.dir, "vault_conflicts")
    private val mutex = Mutex()
    private var index: Index = loadIndex()
    private val _state = MutableStateFlow(stateOf(index, null))
    override val state: StateFlow<MarkdownVaultState> = _state.asStateFlow()

    private val triggers = Channel<Unit>(Channel.CONFLATED)
    private var scope: CoroutineScope? = null
    private var watcher: WatchService? = null
    private val watchedDirs = HashMap<WatchKey, Path>()

    // ── Lifecycle ───────────────────────────────────────────────────────────────────────────────

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        this.scope = scope
        scope.launch(Dispatchers.IO) {
            merge(
                triggers.receiveAsFlow(),
                notesDao.observeNotesFingerprint().drop(1).map { },
                notesDao.observeNotebooksFingerprint().drop(1).map { }
            ).debounce(400).collect { runPass() }
        }
        scope.launch(Dispatchers.IO) {
            while (true) { delay(60_000); triggers.trySend(Unit) }
        }
        scope.launch(Dispatchers.IO) { runPass() }
    }

    override fun syncNow() { triggers.trySend(Unit) }

    /** The notebook the vault folder itself maps to, when the vault is on. */
    fun rootNotebookSyncId(): String? = index.takeIf { it.enabled }?.folders?.get("")

    /** Tests: point at [root] and run one pass now, without the watcher or the timers. */
    internal suspend fun syncOnceForTest(root: File?): MarkdownVaultState {
        if (root != null) mutex.withLock {
            val path = root.canonicalPath
            index = if (path == index.root) index.copy(enabled = true) else Index(enabled = true, root = path)
        }
        mutex.withLock { reconcile() }
        stopWatching()
        publish(null)
        return state.value
    }

    override fun setEnabled(enabled: Boolean) {
        scope?.launch(Dispatchers.IO) {
            mutex.withLock { index = index.copy(enabled = enabled && index.root != null); saveIndex() }
            if (!enabled) stopWatching()
            publish(null)
            triggers.trySend(Unit)
        }
    }

    override fun chooseFolder() {
        scope?.launch(Dispatchers.IO) {
            val picked = pickDirectory(index.root) ?: return@launch
            val dir = File(picked)
            if (!dir.isDirectory) return@launch
            mutex.withLock {
                val path = dir.canonicalPath
                // A different folder starts a fresh mapping (a new notebook); the same one keeps it.
                index = if (path == index.root) index.copy(enabled = true)
                else Index(enabled = true, root = path)
                saveIndex()
            }
            stopWatching()
            publish(null)
            triggers.trySend(Unit)
        }
    }

    private suspend fun runPass() {
        val error = runCatching { mutex.withLock { reconcile() } }.exceptionOrNull()
        if (error != null) System.err.println("Markdown vault: ${error.stackTraceToString()}")
        publish(error?.message ?: error?.javaClass?.simpleName)
    }

    private fun publish(error: String?) {
        _state.value = stateOf(index, error)
    }

    private fun stateOf(i: Index, error: String?) =
        MarkdownVaultState(i.enabled, i.root, i.notes.size, i.lastSyncAt, error)

    // ── The pass ────────────────────────────────────────────────────────────────────────────────

    private suspend fun reconcile() {
        val rootPath = index.root
        if (!index.enabled || rootPath == null) return
        val root = File(rootPath)
        if (!root.isDirectory) error("Folder not found: $rootPath")
        // An unmounted drive leaves an empty mountpoint. Never read "every file is gone" as "delete
        // every note": if none of the files we know about exist, stop and touch nothing.
        if (index.notes.isNotEmpty() && index.notes.values.none { File(root, it.path).isFile }) {
            error("None of the vault's files are in $rootPath — is the drive mounted? Not syncing.")
        }

        val folders = index.folders.toMutableMap()
        val notes = index.notes.toMutableMap()

        // Folders ↔ notebooks.
        val dirs = listDirs(root)
        val notebookByFolder = HashMap<String, Notebook>()
        for (rel in dirs) { // parents before children (sorted by depth)
            val existing = folders[rel]?.let { notesDao.getNotebookBySyncId(it) }
            // A notebook locked in Bori (or under a locked one) leaves the vault: no more plain-text copies.
            if (existing != null && existing.deletedAt == null &&
                (existing.isPrivate || (rel != "" && notebookByFolder[parentOf(rel)] == null))) {
                if (rel == "") { index = index.copy(enabled = false); saveIndex(); return }
                unlinkLocked(root, rel, folders, notes)
                continue
            }
            if (existing != null && existing.deletedAt == null) {
                notebookByFolder[rel] = existing
                continue
            }
            if (existing != null && existing.deletedAt != null) {
                if (rel == "") { // the whole vault notebook was deleted in Bori: stop, touch nothing
                    index = index.copy(enabled = false); saveIndex(); return
                }
                moveToTrash(root, File(root, rel))
                folders.remove(rel)
                notes.entries.removeIf { it.value.path.startsWith("$rel/") }
                continue
            }
            if (rel != "" && notebookByFolder[parentOf(rel)] == null) continue // parent was trashed
            val name = if (rel == "") root.name else rel.substringAfterLast('/')
            val parentId = if (rel == "") null else notebookByFolder[parentOf(rel)]?.id
            val nb = Notebook(name = name, parentId = parentId)
            val id = notesDao.insertNotebook(nb)
            notebookByFolder[rel] = nb.copy(id = id)
            folders[rel] = nb.syncId
        }
        // Folders gone from disk: their notebook goes to Recently Deleted once it holds no live notes.
        for ((rel, syncId) in folders.entries.toList()) {
            if (rel in notebookByFolder || rel == "") continue
            if (File(root, rel).isDirectory) continue // skipped above (trashed parent)
            folders.remove(rel)
            val nb = notesDao.getNotebookBySyncId(syncId) ?: continue
            val live = notesDao.getNotesInNotebooksIncludingDeleted(listOf(nb.id)).any { it.deletedAt == null }
            if (!live && nb.deletedAt == null) {
                val now = System.currentTimeMillis()
                notesDao.updateNotebook(nb.copy(deletedAt = now, updatedAt = now))
            }
        }
        // Child notebooks made in Bori under a vault notebook become folders.
        val allNotebooks = notesDao.getAllNotebooksForBackup()
        var grew = true
        while (grew) {
            grew = false
            for (nb in allNotebooks) {
                if (nb.deletedAt != null || nb.isPrivate || notebookByFolder.values.any { it.id == nb.id }) continue
                val parentRel = notebookByFolder.entries.firstOrNull { it.value.id == nb.parentId }?.key ?: continue
                val rel = uniqueChild(root, parentRel, sanitize(nb.name), isDir = true)
                File(root, rel).mkdirs()
                notebookByFolder[rel] = nb
                folders[rel] = nb.syncId
                grew = true
            }
        }
        // Notebook renamed in Bori → folder renamed.
        // Shallowest first, and each path looked up again: a parent's rename moves its children's keys.
        for (nb in notebookByFolder.entries.sortedBy { it.key.count { c -> c == '/' } }.map { it.value }) {
            val rel = notebookByFolder.entries.first { it.value.id == nb.id }.key
            if (rel == "" || sanitize(nb.name) == rel.substringAfterLast('/')) continue
            val newRel = uniqueChild(root, parentOf(rel), sanitize(nb.name), isDir = true)
            Files.move(File(root, rel).toPath(), File(root, newRel).toPath())
            renamePrefix(rel, newRel, folders, notes, notebookByFolder)
        }
        val folderOfNotebook = notebookByFolder.entries.associate { it.value.id to it.key }

        // Files ↔ notes.
        val files = listMarkdown(root).associateWith { File(root, it) }
        val pathToId = notes.entries.associate { it.value.path to it.key }
        val missing = notes.filterValues { it.path !in files }.toMutableMap()
        val hashes = HashMap<String, String>()
        fun hashOf(rel: String) = hashes.getOrPut(rel) { sha256(files.getValue(rel).readBytes()) }

        val handled = HashSet<String>()
        for ((rel, file) in files) {
            var id = pathToId[rel]
            var moved = false
            if (id == null) {
                // Moved or renamed outside Bori: an unknown file with a vanished file's exact content.
                val gone = missing.entries.firstOrNull { it.value.hash == hashOf(rel) }
                if (gone != null) {
                    id = gone.key
                    missing.remove(id)
                    notes[id] = gone.value.copy(path = rel)
                    moved = true
                }
            }
            if (id == null) continue // new file, handled below
            handled += id
            val entry = notes.getValue(id)
            val note = notesDao.getNoteBySyncId(id)
            if (note == null) { // removed from Bori for good
                moveToTrash(root, file); notes.remove(id); continue
            }
            // A move outside Bori counts as a file change: the title or notebook comes from the path.
            val fileChanged = moved || hashOf(rel) != entry.hash
            val noteChanged = note.updatedAt != entry.noteUpdatedAt
            when {
                fileChanged && noteChanged && note.deletedAt == null -> {
                    if (file.lastModified() > note.updatedAt) {
                        saveConflict(rel, write(null, note), "bori")
                        notes[id] = importFile(rel, file, note, notebookByFolder).second
                    } else {
                        saveConflict(rel, file.readText(), "file")
                        exportNote(root, note, rel, folderOfNotebook)?.let { notes[id] = it } ?: notes.remove(id)
                    }
                }
                fileChanged -> if (note.deletedAt == null) notes[id] = importFile(rel, file, note, notebookByFolder).second
                    else { exportNote(root, note, rel, folderOfNotebook); notes.remove(id) }
                noteChanged -> exportNote(root, note, rel, folderOfNotebook)?.let { notes[id] = it } ?: notes.remove(id)
                note.notebookId !in folderOfNotebook -> { moveToTrash(root, file); notes.remove(id) }
            }
        }

        // New files → new notes (or a same-titled note in that notebook that isn't linked yet).
        val linkedIds = notes.keys.toSet()
        for ((rel, file) in files) {
            if (rel in pathToId || notes.values.any { it.path == rel }) continue
            val nb = notebookByFolder[parentOf(rel)] ?: continue
            val title = file.name.removeSuffix(".md")
            val twin = notesDao.getNotesInNotebooksIncludingDeleted(listOf(nb.id))
                .firstOrNull { it.deletedAt == null && it.title == title && it.syncId !in linkedIds }
            val (syncId, entry) = importFile(rel, file, twin, notebookByFolder)
            notes[syncId] = entry
        }

        // Files deleted outside Bori → the note goes to Recently Deleted.
        for ((id, _) in missing) {
            if (id in handled) continue
            notes.remove(id)
            val note = notesDao.getNoteBySyncId(id) ?: continue
            if (note.deletedAt == null) {
                val now = System.currentTimeMillis()
                notesDao.updateNote(note.copy(deletedAt = now, updatedAt = now))
            }
        }

        // Notes made in Bori (or synced from the phone) in a vault notebook → new files.
        val vaultNotes = notesDao.getNotesInNotebooksIncludingDeleted(folderOfNotebook.keys.toList())
        for (note in vaultNotes) {
            if (note.syncId in notes || note.deletedAt != null || isBlankNote(note)) continue
            exportNote(root, note, null, folderOfNotebook)?.let { notes[note.syncId] = it }
        }

        index = index.copy(folders = folders, notes = notes, lastSyncAt = System.currentTimeMillis())
        saveIndex()
        watch(root, notebookByFolder.keys)
    }

    /** File → note. Creates the note when [note] is null. Returns the note's syncId and its index entry. */
    private suspend fun importFile(
        rel: String, file: File, note: Note?, notebookByFolder: Map<String, Notebook>
    ): Pair<String, Entry> {
        val text = file.readText()
        val parsed = VaultMarkdown.parse(text)
        val notebook = notebookByFolder.getValue(parentOf(rel))
        val title = file.name.removeSuffix(".md")
        val blocks = parsed.toBlocks()
        val content = serializeBlocks(blocks)
        val now = System.currentTimeMillis()
        val updatedAt = maxOf(now, (note?.updatedAt ?: 0) + 1)
        val updated = (note ?: Note(notebookId = notebook.id, title = title, content = "", createdAt = file.lastModified().coerceAtMost(now)))
            .copy(
                notebookId = notebook.id,
                title = title,
                content = content,
                tags = parsed.tags.joinToString(","),
                bodyPreview = preview(blocks),
                updatedAt = updatedAt,
                deletedAt = null
            )
        if (note == null) notesDao.insertNote(updated) else notesDao.updateNote(updated)
        return updated.syncId to Entry(rel, sha256(text.toByteArray()), updatedAt)
    }

    /**
     * Note → file. [currentRel] is where its file is now (null: it has none yet). Handles rename,
     * move and delete. Returns the new index entry, or null when the note no longer has a file.
     */
    private fun exportNote(root: File, note: Note, currentRel: String?, folderOfNotebook: Map<Long, String>): Entry? {
        val current = currentRel?.let { File(root, it) }
        val folder = folderOfNotebook[note.notebookId]
        if (note.deletedAt != null || folder == null) {
            if (current != null && current.exists()) moveToTrash(root, current)
            return null
        }
        val wantedName = sanitize(note.title.ifBlank { "Untitled" })
        var rel = currentRel
        if (rel == null || parentOf(rel) != folder || File(rel).name.removeSuffix(".md") != wantedName) {
            val target = uniqueChild(root, folder, wantedName, isDir = false, except = currentRel)
            if (current != null && current.exists() && target != currentRel) {
                File(root, target).parentFile.mkdirs()
                Files.move(current.toPath(), File(root, target).toPath())
            }
            rel = target
        }
        val file = File(root, rel)
        val previous = if (file.exists()) file.readText() else null
        val text = write(previous, note)
        if (text != previous) atomicWrite(file, text)
        return Entry(rel, sha256(text.toByteArray()), note.updatedAt)
    }

    private fun write(previous: String?, note: Note): String =
        VaultMarkdown.write(previous, note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }, deserializeBlocks(note.content))

    // ── Files ───────────────────────────────────────────────────────────────────────────────────

    private fun skip(name: String) = name.startsWith(".") || name == "node_modules"

    /** Folders relative to [root], "" first, parents before children. */
    private fun listDirs(root: File): List<String> =
        root.walkTopDown().onEnter { it == root || !skip(it.name) }
            .filter { it.isDirectory }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .sortedBy { if (it.isEmpty()) 0 else it.count { c -> c == '/' } + 1 }
            .toList()

    private fun listMarkdown(root: File): List<String> =
        root.walkTopDown().onEnter { it == root || !skip(it.name) }
            .filter { it.isFile && it.name.endsWith(".md") && !skip(it.name) }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .toList()

    private fun parentOf(rel: String) = if ('/' in rel) rel.substringBeforeLast('/') else ""

    /** Characters Obsidian, Windows or a file system won't take in a name. */
    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|#^\\[\\]]"), " ").replace(Regex("\\s+"), " ").trim().trimStart('.')
            .take(120).ifBlank { "Untitled" }

    private fun uniqueChild(root: File, folder: String, base: String, isDir: Boolean, except: String? = null): String {
        val ext = if (isDir) "" else ".md"
        var n = 1
        while (true) {
            val name = if (n == 1) "$base$ext" else "$base $n$ext"
            val rel = if (folder.isEmpty()) name else "$folder/$name"
            if (rel == except || !File(root, rel).exists()) return rel
            n++
        }
    }

    /**
     * A notebook got locked: delete the plain-text copies of its notes (not to .trash, which would keep
     * them readable) and forget the folder. The notes themselves stay in Bori. A file edited since the
     * last pass, or anything that isn't one of our notes, is left where it is.
     */
    private fun unlinkLocked(root: File, rel: String, folders: MutableMap<String, String>, notes: MutableMap<String, Entry>) {
        for (entry in notes.values) {
            if (!entry.path.startsWith("$rel/")) continue
            val f = File(root, entry.path)
            if (f.isFile && sha256(f.readBytes()) == entry.hash) f.delete()
        }
        File(root, rel).takeIf { it.isDirectory }?.walkBottomUp()?.forEach { if (it.isDirectory) it.delete() } // only empties
        folders.keys.removeIf { it == rel || it.startsWith("$rel/") }
        notes.entries.removeIf { it.value.path.startsWith("$rel/") }
    }

    private fun renamePrefix(
        from: String, to: String, folders: MutableMap<String, String>, notes: MutableMap<String, Entry>,
        notebookByFolder: HashMap<String, Notebook>
    ) {
        fun re(p: String) = if (p == from) to else if (p.startsWith("$from/")) to + p.removePrefix(from) else p
        folders.keys.toList().forEach { k -> val v = folders.remove(k)!!; folders[re(k)] = v }
        notes.keys.toList().forEach { k -> notes[k] = notes.getValue(k).let { it.copy(path = re(it.path)) } }
        notebookByFolder.keys.toList().forEach { k -> val v = notebookByFolder.remove(k)!!; notebookByFolder[re(k)] = v }
    }

    private fun atomicWrite(file: File, text: String) {
        file.parentFile.mkdirs()
        val tmp = File(file.parentFile, ".${file.name}.bori-tmp")
        tmp.writeText(text)
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    /** Obsidian's own trash folder, so a note deleted in Bori can still be found in the vault. */
    private fun moveToTrash(root: File, file: File) {
        if (!file.exists()) return
        val trash = File(root, ".trash").apply { mkdirs() }
        var target = File(trash, file.name)
        var n = 2
        while (target.exists()) {
            target = File(trash, if (file.isDirectory) "${file.name} $n" else "${file.nameWithoutExtension} $n.${file.extension}")
            n++
        }
        Files.move(file.toPath(), target.toPath())
    }

    private fun saveConflict(rel: String, text: String, side: String) {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
        val f = File(conflictsDir, "$stamp/$side/$rel")
        f.parentFile.mkdirs()
        f.writeText(text)
    }

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    // ── Watching ────────────────────────────────────────────────────────────────────────────────

    private fun watch(root: File, rels: Collection<String>) {
        val ws = watcher ?: FileSystems.getDefault().newWatchService().also { ws ->
            watcher = ws
            Thread({
                try {
                    while (true) {
                        val key = ws.take()
                        key.pollEvents()
                        if (!key.reset()) synchronized(watchedDirs) { watchedDirs.remove(key) }
                        triggers.trySend(Unit)
                    }
                } catch (_: ClosedWatchServiceException) {
                } catch (_: InterruptedException) {
                }
            }, "bori-vault-watch").apply { isDaemon = true }.start()
        }
        synchronized(watchedDirs) {
            val already = watchedDirs.values.toSet()
            for (rel in rels) {
                val p = File(root, rel).toPath()
                if (p in already || !Files.isDirectory(p)) continue
                val key = p.register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY)
                watchedDirs[key] = p
            }
        }
    }

    private fun stopWatching() {
        runCatching { watcher?.close() }
        watcher = null
        synchronized(watchedDirs) { watchedDirs.clear() }
    }

    // ── Index ───────────────────────────────────────────────────────────────────────────────────

    private fun loadIndex(): Index =
        runCatching { json.decodeFromString(Index.serializer(), indexFile.readText()) }.getOrDefault(Index())

    private fun saveIndex() {
        val tmp = File(indexFile.parentFile, indexFile.name + ".tmp")
        tmp.writeText(json.encodeToString(Index.serializer(), index))
        Files.move(tmp.toPath(), indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    // ── Blocks JSON (the editor's format) ───────────────────────────────────────────────────────

    private fun isBlankNote(note: Note) = note.title.isBlank() && deserializeBlocks(note.content).all {
        it.content.isBlank() && it.type != BlockType.DIVIDER && it.type != BlockType.IMAGE && it.type != BlockType.INK
    }

    private fun preview(blocks: List<NoteBlock>) = NoteBlocksJson.preview(blocks)
    private fun serializeBlocks(blocks: List<NoteBlock>) = NoteBlocksJson.encode(blocks)
    private fun deserializeBlocks(json: String) = NoteBlocksJson.decode(json)

    // ── Folder picker ───────────────────────────────────────────────────────────────────────────

    private fun pickDirectory(start: String?): String? {
        val from = start ?: System.getProperty("user.home")
        for (cmd in listOf(
            listOf("kdialog", "--getexistingdirectory", from, "--title", "Choose your Markdown / Obsidian folder"),
            listOf("zenity", "--file-selection", "--directory", "--filename=$from/", "--title=Choose your Markdown / Obsidian folder")
        )) {
            val result = runCatching {
                val p = ProcessBuilder(cmd).redirectErrorStream(false).start()
                val out = p.inputStream.bufferedReader().readText().trim()
                if (p.waitFor() == 0 && out.isNotEmpty()) out else ""
            }.getOrNull() ?: continue // tool not installed: try the next
            return result.ifEmpty { null } // cancelled
        }
        var chosen: String? = null
        javax.swing.SwingUtilities.invokeAndWait {
            val chooser = javax.swing.JFileChooser(from).apply {
                fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Choose your Markdown / Obsidian folder"
            }
            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) chosen = chooser.selectedFile.path
        }
        return chosen
    }
}
