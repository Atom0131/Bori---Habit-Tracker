package com.apagon.rhythm.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.repository.NotesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

data class NoteSearchResult(val note: Note, val notebookName: String)
class NotesSearchViewModel constructor(
    private val repository: NotesRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val results: StateFlow<List<NoteSearchResult>> = combine(
        _query
            .debounce(250)
            .flatMapLatest { q ->
                if (q.length < 2) flowOf(emptyList())
                else repository.searchAllNotes(q)
            },
        repository.getAllNotebooks()
    ) { notes, notebooks ->
        val notebookNames = notebooks.associate { it.id to it.name }
        notes.map { note -> NoteSearchResult(note, notebookNames[note.notebookId] ?: "") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
}
