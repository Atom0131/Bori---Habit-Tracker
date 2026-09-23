package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.NotesDao
import com.apagon.rhythm.data.db.NoteCountResult
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.Notebook
import kotlinx.coroutines.flow.Flow
class NotesRepository constructor(
    private val notesDao: NotesDao
) {
    // Notebooks
    fun getAllNotebooks(): Flow<List<Notebook>> = notesDao.getAllNotebooks()
    suspend fun insertNotebook(notebook: Notebook): Long = notesDao.insertNotebook(notebook)
    suspend fun updateNotebook(notebook: Notebook) = notesDao.updateNotebook(notebook)
    suspend fun deleteNotebook(notebook: Notebook) = notesDao.updateNotebook(notebook.copy(deletedAt = System.currentTimeMillis()))
    suspend fun hardDeleteNotebook(notebook: Notebook) = notesDao.deleteNotebook(notebook)
    fun getDeletedNotebooks(): Flow<List<Notebook>> = notesDao.getDeletedNotebooks()
    suspend fun getNotebookById(id: Long): Notebook? = notesDao.getNotebookById(id)

    // Notes
    fun getNotesByNotebook(notebookId: Long): Flow<List<Note>> = notesDao.getNotesByNotebook(notebookId)
    suspend fun getNoteById(id: Long): Note? = notesDao.getNoteById(id)
    suspend fun insertNote(note: Note): Long = notesDao.insertNote(note)
    suspend fun updateNote(note: Note) = notesDao.updateNote(note)
    suspend fun deleteNote(note: Note) = notesDao.updateNote(note.copy(deletedAt = System.currentTimeMillis()))
    suspend fun hardDeleteNote(note: Note) = notesDao.deleteNote(note)
    fun getDeletedNotes(): Flow<List<Note>> = notesDao.getDeletedNotes()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        notesDao.purgeDeletedNotebooks(olderThan)
        notesDao.purgeDeletedNotes(olderThan)
    }

    fun searchAllNotes(query: String): Flow<List<Note>> = notesDao.searchAllNotes(query)
    fun getNoteCounts(): Flow<List<NoteCountResult>> = notesDao.getNoteCounts()
}
