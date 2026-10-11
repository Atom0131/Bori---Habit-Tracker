@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.apagon.rhythm.ui.notes

import com.apagon.rhythm.core.time.System
import com.apagon.rhythm.platform.PhotoStorage

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.repository.NotesRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.apagon.rhythm.core.json.JSONArray
import com.apagon.rhythm.core.json.JSONObject
import kotlin.uuid.Uuid

// Stage 10: mechanical move from androidMain to commonMain — this file had
// zero real Android dependencies (PhotoStorage is already a cross-platform
// DI interface), only java.util.UUID (JVM-only, would break the iOS targets
// this project still declares even though iosMain is empty) and a bare
// System.currentTimeMillis() call that relied on androidMain's implicit
// java.lang.System import. Both fixed below; no behavior change.
enum class BlockType { TEXT, HEADER, CHECKLIST, BULLET_LIST, NUMBERED_LIST, QUOTE, CODE, DIVIDER, IMAGE }

data class NoteBlock(
    val id: String = Uuid.random().toString(),
    val type: BlockType = BlockType.TEXT,
    val content: String = "",
    val isChecked: Boolean = false
)
class NoteEditorViewModel constructor(
    private val photoStorage: PhotoStorage,
    private val repository: NotesRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private companion object {
        /** Idle time before an edit is written — short, so little is ever in flight. */
        const val AUTOSAVE_DEBOUNCE_MS = 500L
    }

    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _blocks = MutableStateFlow<List<NoteBlock>>(listOf(NoteBlock()))
    val blocks: StateFlow<List<NoteBlock>> = _blocks.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _fontFamily = MutableStateFlow("default")
    val fontFamily: StateFlow<String> = _fontFamily.asStateFlow()

    private val _fontSize = MutableStateFlow("normal")
    val fontSize: StateFlow<String> = _fontSize.asStateFlow()

    val allNotebooks: StateFlow<List<Notebook>> = repository.getAllNotebooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wordCount: StateFlow<Int> = _blocks.map { blocks ->
        blocks.filter { it.type != BlockType.IMAGE && it.type != BlockType.DIVIDER }
            .joinToString(" ") { it.content }
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val charCount: StateFlow<Int> = _blocks.map { blocks ->
        blocks.filter { it.type != BlockType.IMAGE && it.type != BlockType.DIVIDER }
            .sumOf { it.content.length }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val toolbarPinned: StateFlow<Boolean> = themePreferences.notesToolbarPinned
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadNote(noteId: Long, notebookId: Long, template: NoteTemplate? = null) {
        viewModelScope.launch {
            saveMutex.withLock {
                // One editor serves every note in the window, and the notebook rail leaves it without
                // calling saveNote — so settle the previous note first. Never the blank-note cleanup when
                // the note being opened IS that note: reopening an empty new note would delete it, and
                // whatever was typed next would go into a row that no longer exists.
                if (noteId != -1L && noteId == _note.value?.id) persistIfChanged() else finishCurrent()
                if (noteId == -1L) {
                    _note.value = Note(notebookId = notebookId, title = "", content = "")
                    _title.value = ""
                    _blocks.value = template?.buildBlocks() ?: listOf(NoteBlock())
                    _tags.value = emptyList()
                    createdHere = true
                    persist() // saved the moment it opens, so nothing typed into it can be lost
                } else {
                    val n = repository.getNoteById(noteId)
                    _note.value = n
                    _title.value = n?.title ?: ""
                    _blocks.value = deserializeBlocks(n?.content ?: "")
                    _tags.value = parseTags(n?.tags ?: "")
                    _fontFamily.value = n?.fontFamily ?: "default"
                    _fontSize.value = n?.fontSize ?: "normal"
                    createdHere = false
                    savedDraft = currentDraft()
                }
            }
        }
    }

    fun updateTitle(newTitle: String) { _title.value = newTitle }

    fun updateBlock(index: Int, updatedBlock: NoteBlock) {
        val current = _blocks.value.toMutableList()
        if (index in current.indices) {
            current[index] = updatedBlock
            _blocks.value = current
        }
    }

    fun addBlock(index: Int, type: BlockType = BlockType.TEXT) {
        val current = _blocks.value.toMutableList()
        current.add(index + 1, NoteBlock(type = type))
        _blocks.value = current
    }

    fun addImageBlock(afterIndex: Int, uri: String) {
        viewModelScope.launch {
            val storedPath = photoStorage.importPhoto(uri, "notes/images")
            val current = _blocks.value.toMutableList()
            current.add(afterIndex + 1, NoteBlock(type = BlockType.IMAGE, content = storedPath ?: ""))
            _blocks.value = current
        }
    }

    fun removeBlock(index: Int) {
        val current = _blocks.value.toMutableList()
        if (current.size > 1 && index in current.indices) {
            current.removeAt(index)
            _blocks.value = current
        }
    }

    fun addTag(tag: String) {
        val t = tag.trim().lowercase().replace(",", "")
        if (t.isNotEmpty() && t !in _tags.value) _tags.value = _tags.value + t
    }

    fun removeTag(tag: String) { _tags.value = _tags.value - tag }

    fun setFontFamily(key: String) { _fontFamily.value = key }
    fun setFontSize(key: String) { _fontSize.value = key }

    fun moveNote(targetNotebookId: Long) {
        viewModelScope.launch {
            saveMutex.withLock {
                val current = _note.value ?: return@withLock
                _note.value = current.copy(notebookId = targetNotebookId)
                persist()
            }
        }
    }

    fun setToolbarPinned(pinned: Boolean) {
        viewModelScope.launch { themePreferences.setNotesToolbarPinned(pinned) }
    }

    fun applyFormattingToBlock(blockIndex: Int, selection: TextRange, openMarker: String, closeMarker: String = openMarker) {
        val blocks = _blocks.value.toMutableList()
        val block = blocks.getOrNull(blockIndex) ?: return
        val content = block.content
        val start = selection.min.coerceIn(0, content.length)
        val end = selection.max.coerceIn(0, content.length)
        val newContent = if (start == end) {
            content.substring(0, start) + openMarker + closeMarker + content.substring(end)
        } else {
            content.substring(0, start) + openMarker + content.substring(start, end) + closeMarker + content.substring(end)
        }
        blocks[blockIndex] = block.copy(content = newContent)
        _blocks.value = blocks
    }

    fun changeBlockType(blockIndex: Int, newType: BlockType) {
        val blocks = _blocks.value.toMutableList()
        val block = blocks.getOrNull(blockIndex) ?: return
        blocks[blockIndex] = block.copy(type = newType)
        _blocks.value = blocks
    }

    fun applyTemplate(template: NoteTemplate) {
        _blocks.value = template.buildBlocks()
    }

    fun exportMarkdown(): String {
        val sb = StringBuilder()
        if (_title.value.isNotBlank()) {
            sb.appendLine("# ${_title.value}")
            sb.appendLine()
        }
        if (_tags.value.isNotEmpty()) {
            sb.appendLine("Tags: ${_tags.value.joinToString(", ")}")
            sb.appendLine()
        }
        var numberedCounter = 0
        _blocks.value.forEach { block ->
            if (block.type != BlockType.NUMBERED_LIST) numberedCounter = 0
            when (block.type) {
                BlockType.TEXT -> if (block.content.isNotBlank()) sb.appendLine(block.content)
                BlockType.HEADER -> if (block.content.isNotBlank()) sb.appendLine("## ${block.content}")
                BlockType.CHECKLIST -> sb.appendLine("- ${if (block.isChecked) "[x]" else "[ ]"} ${block.content}")
                BlockType.BULLET_LIST -> sb.appendLine("- ${block.content}")
                BlockType.NUMBERED_LIST -> { numberedCounter++; sb.appendLine("$numberedCounter. ${block.content}") }
                BlockType.QUOTE -> if (block.content.isNotBlank()) sb.appendLine("> ${block.content}")
                BlockType.CODE -> if (block.content.isNotBlank()) {
                    sb.appendLine("```")
                    sb.appendLine(block.content)
                    sb.appendLine("```")
                }
                BlockType.DIVIDER -> sb.appendLine("---")
                BlockType.IMAGE -> {
                    val filename = block.content.substringAfterLast('/').ifBlank { "image" }
                    sb.appendLine("![image]($filename)")
                }
            }
        }
        return sb.toString().trimEnd()
    }

    /**
     * **Autosave — the same promise as the Android editor: what you type is kept.** A note used to
     * reach the database only through the back arrow or the overflow Save item; leaving any other
     * way (the sidebar, closing the window, the app being restarted) lost it, and that is how a note
     * written in the "Rhy" notebook vanished on 2026-10-11.
     *
     * Now a new note is inserted the moment it opens ([loadNote]), and every change — title, blocks,
     * tags, fonts, whichever function made it — is written [AUTOSAVE_DEBOUNCE_MS] after typing stops,
     * by watching the editor state itself rather than each mutator. [savedDraft] keeps a no-op (a
     * load, a re-emit) from touching `updatedAt`. [saveMutex] keeps the debounce, [loadNote] and
     * [saveNote] from racing a second insert.
     */
    private data class Draft(
        val title: String,
        val blocks: List<NoteBlock>,
        val tags: List<String>,
        val fontFamily: String,
        val fontSize: String
    )

    private val saveMutex = Mutex()
    private var savedDraft: Draft? = null
    /** True for a note this editor created, so leaving it still blank can remove it again. */
    private var createdHere = false

    private fun currentDraft() =
        Draft(_title.value, _blocks.value, _tags.value, _fontFamily.value, _fontSize.value)

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            combine(_title, _blocks, _tags, _fontFamily, _fontSize) { _, _, _, _, _ -> Unit }
                .debounce(AUTOSAVE_DEBOUNCE_MS)
                .collect { saveMutex.withLock { persistIfChanged() } }
        }
    }

    private suspend fun persistIfChanged() {
        if (_note.value != null && currentDraft() != savedDraft) persist()
    }

    /** Insert or update the current note. Caller holds [saveMutex]. */
    private suspend fun persist() {
        val currentNote = _note.value ?: return
        val draft = currentDraft()
        val updatedNote = currentNote.copy(
            title = draft.title,
            content = serializeBlocks(draft.blocks),
            tags = draft.tags.joinToString(","),
            fontFamily = draft.fontFamily,
            fontSize = draft.fontSize,
            updatedAt = System.currentTimeMillis()
        )
        if (updatedNote.id == 0L) {
            val id = repository.insertNote(updatedNote)
            _note.value = updatedNote.copy(id = id) // later saves update, never re-insert
        } else {
            repository.updateNote(updatedNote)
            _note.value = updatedNote
        }
        savedDraft = draft
    }

    private fun Draft.isBlank() = title.isBlank() && tags.isEmpty() &&
        blocks.all { it.type != BlockType.IMAGE && it.type != BlockType.DIVIDER && it.content.isBlank() }

    /** Write any pending change; a note created here and still completely empty is removed. Caller holds [saveMutex]. */
    private suspend fun finishCurrent() {
        val n = _note.value ?: return
        if (createdHere && n.id != 0L && currentDraft().isBlank()) {
            repository.hardDeleteNote(n)
            _note.value = null
            savedDraft = null
        } else {
            persistIfChanged()
        }
    }

    /** Save now (every exit path). A note opened here and left completely empty is removed instead. */
    fun saveNote(onComplete: () -> Unit) {
        viewModelScope.launch {
            saveMutex.withLock { finishCurrent() }
            onComplete()
        }
    }

    private fun serializeBlocks(blocks: List<NoteBlock>): String {
        val array = JSONArray()
        blocks.forEach { block ->
            val obj = JSONObject()
            obj.put("id", block.id)
            obj.put("type", block.type.name)
            obj.put("content", block.content)
            obj.put("isChecked", block.isChecked)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseTags(s: String) = s.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    private fun deserializeBlocks(json: String): List<NoteBlock> {
        if (json.isBlank()) return listOf(NoteBlock())
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<NoteBlock>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeStr = obj.optString("type", "TEXT").uppercase()
                val type = runCatching { BlockType.valueOf(typeStr) }.getOrDefault(BlockType.TEXT)
                list.add(NoteBlock(
                    id = obj.optString("id", Uuid.random().toString()),
                    type = type,
                    content = obj.optString("content", ""),
                    isChecked = obj.optBoolean("isChecked", false)
                ))
            }
            if (list.isEmpty()) listOf(NoteBlock()) else list
        } catch (e: Exception) {
            listOf(NoteBlock(content = json))
        }
    }
}
