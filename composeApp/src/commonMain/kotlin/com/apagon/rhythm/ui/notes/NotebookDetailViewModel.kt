package com.apagon.rhythm.ui.notes

import com.apagon.rhythm.core.time.System

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.repository.NotesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class NoteSort { NEWEST, OLDEST, A_TO_Z, Z_TO_A, PINNED_FIRST }
class NotebookDetailViewModel constructor(
    private val repository: NotesRepository
) : ViewModel() {

    private val _notebookId = MutableStateFlow<Long?>(null)

    val notebook: StateFlow<Notebook?> = _notebookId
        .filterNotNull()
        .map { repository.getNotebookById(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _rawNotes: StateFlow<List<Note>> = _notebookId
        .filterNotNull()
        .flatMapLatest { repository.getNotesByNotebook(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(NoteSort.NEWEST)
    val sortMode: StateFlow<NoteSort> = _sortMode.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Long>> = _selectedNoteIds.asStateFlow()

    val isInSelectMode: StateFlow<Boolean> = _selectedNoteIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val allNotebooks: StateFlow<List<Notebook>> = repository.getAllNotebooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<String>> = _rawNotes
        .map { list -> list.flatMap { parseTags(it.tags) }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = combine(
        _rawNotes, _selectedTag, _searchQuery, _sortMode
    ) { all, tag, query, sort ->
        val filtered = all.filter { note ->
            val tagOk = tag == null || parseTags(note.tags).contains(tag)
            val queryOk = query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                parseNotePreview(note.content).contains(query, ignoreCase = true)
            tagOk && queryOk
        }
        when (sort) {
            NoteSort.NEWEST -> filtered.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
            NoteSort.OLDEST -> filtered.sortedWith(compareByDescending<Note> { it.isPinned }.thenBy { it.updatedAt })
            NoteSort.A_TO_Z -> filtered.sortedWith(compareByDescending<Note> { it.isPinned }.thenBy { it.title.lowercase() })
            NoteSort.Z_TO_A -> filtered.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.title.lowercase() })
            NoteSort.PINNED_FIRST -> filtered.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setNotebookId(id: Long) { _notebookId.value = id }
    fun setTag(tag: String?) { _selectedTag.value = tag }
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setSort(mode: NoteSort) { _sortMode.value = mode }

    fun toggleSelection(noteId: Long) {
        _selectedNoteIds.value = if (noteId in _selectedNoteIds.value) {
            _selectedNoteIds.value - noteId
        } else {
            _selectedNoteIds.value + noteId
        }
    }

    fun clearSelection() { _selectedNoteIds.value = emptySet() }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun moveNote(note: Note, targetNotebookId: Long) {
        viewModelScope.launch {
            repository.updateNote(note.copy(notebookId = targetNotebookId, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteSelectedNotes() {
        viewModelScope.launch {
            _rawNotes.value.filter { it.id in _selectedNoteIds.value }.forEach {
                repository.deleteNote(it)
            }
            clearSelection()
        }
    }

    fun moveSelectedNotes(targetNotebookId: Long) {
        viewModelScope.launch {
            _rawNotes.value.filter { it.id in _selectedNoteIds.value }.forEach {
                repository.updateNote(it.copy(notebookId = targetNotebookId, updatedAt = System.currentTimeMillis()))
            }
            clearSelection()
        }
    }

    fun addTagToSelectedNotes(tag: String) {
        viewModelScope.launch {
            val t = tag.trim().lowercase()
            if (t.isEmpty()) return@launch
            _rawNotes.value.filter { it.id in _selectedNoteIds.value }.forEach { note ->
                val existing = parseTags(note.tags)
                if (t !in existing) {
                    repository.updateNote(note.copy(tags = (existing + t).joinToString(",")))
                }
            }
            clearSelection()
        }
    }

    private fun parseTags(s: String) = s.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    private fun parseNotePreview(json: String): String {
        if (json.isBlank()) return ""
        return try {
            val array = com.apagon.rhythm.core.json.JSONArray(json)
            val sb = StringBuilder()
            val textTypes = setOf("text", "todo", "checklist", "header", "bullet_list", "numbered_list", "quote", "code")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = obj.optString("type", "text").lowercase()
                val content = obj.optString("content", "")
                if (type in textTypes && content.isNotBlank()) {
                    if (sb.isNotEmpty()) sb.append(" ")
                    sb.append(content)
                }
            }
            sb.toString()
        } catch (e: Exception) {
            json
        }
    }
}
