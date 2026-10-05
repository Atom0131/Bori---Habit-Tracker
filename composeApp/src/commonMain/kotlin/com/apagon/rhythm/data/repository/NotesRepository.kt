package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.NotesDao
import com.apagon.rhythm.data.db.NoteCountResult
import com.apagon.rhythm.data.db.NoteLinkDao
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.model.NoteLink
import kotlinx.coroutines.flow.Flow
class NotesRepository constructor(
    private val notesDao: NotesDao,
    private val noteLinkDao: NoteLinkDao
) {
    // Notebooks
    fun getAllNotebooks(): Flow<List<Notebook>> = notesDao.getAllNotebooks()
    suspend fun insertNotebook(notebook: Notebook): Long = notesDao.insertNotebook(notebook)
    suspend fun updateNotebook(notebook: Notebook) = notesDao.updateNotebook(notebook.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteNotebook(notebook: Notebook) {
        val now = System.currentTimeMillis()
        notesDao.updateNotebook(notebook.copy(deletedAt = now, updatedAt = now))
    }
    suspend fun hardDeleteNotebook(notebook: Notebook) = notesDao.deleteNotebook(notebook)
    fun getDeletedNotebooks(): Flow<List<Notebook>> = notesDao.getDeletedNotebooks()
    suspend fun getNotebookById(id: Long): Notebook? = notesDao.getNotebookById(id)

    // Notes
    fun getNotesByNotebook(notebookId: Long): Flow<List<Note>> = notesDao.getNotesByNotebook(notebookId)
    suspend fun getNoteById(id: Long): Note? = notesDao.getNoteById(id)
    suspend fun insertNote(note: Note): Long = notesDao.insertNote(note)
    suspend fun updateNote(note: Note) = notesDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteNote(note: Note) {
        val now = System.currentTimeMillis()
        notesDao.updateNote(note.copy(deletedAt = now, updatedAt = now))
    }
    suspend fun hardDeleteNote(note: Note) = notesDao.deleteNote(note)
    fun getDeletedNotes(): Flow<List<Note>> = notesDao.getDeletedNotes()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        notesDao.purgeDeletedNotebooks(olderThan)
        notesDao.purgeDeletedNotes(olderThan)
    }

    fun searchAllNotes(query: String): Flow<List<Note>> = notesDao.searchAllNotes(query)
    fun getNoteCounts(): Flow<List<NoteCountResult>> = notesDao.getNoteCounts()

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────
    // isPrivate is excluded at the query itself (see NotesDao) — not an after-the-fact filter.

    suspend fun getNotebookBySyncId(syncId: String): Notebook? = notesDao.getNotebookBySyncId(syncId)

    suspend fun getNonPrivateNotebooksUpdatedSince(since: Long): List<Notebook> =
        notesDao.getNonPrivateNotebooksUpdatedSince(since)

    suspend fun getUnsyncedNotebookByName(name: String): Notebook? = notesDao.getUnsyncedNotebookByName(name)

    suspend fun insertNotebookFromSync(notebook: Notebook): Long = notesDao.insertNotebook(notebook)

    suspend fun updateNotebookFromSync(notebook: Notebook) = notesDao.updateNotebook(notebook)

    suspend fun getNoteBySyncId(syncId: String): Note? = notesDao.getNoteBySyncId(syncId)

    suspend fun getNonPrivateNotesUpdatedSince(since: Long): List<Note> =
        notesDao.getNonPrivateNotesUpdatedSince(since)

    suspend fun insertNoteFromSync(note: Note): Long = notesDao.insertNote(note)

    suspend fun updateNoteFromSync(note: Note) = notesDao.updateNote(note)

    // ── Sync (Stage 2) — note links, full-replace-per-parent (source note only) ─────────────────

    suspend fun getLinksForSourceNoteSync(sourceNoteId: Long): List<NoteLink> =
        noteLinkDao.getLinksForSourceOnce(sourceNoteId)

    suspend fun replaceLinksForSourceNoteFromSync(sourceNoteId: Long, links: List<NoteLink>) {
        noteLinkDao.deleteLinksForSource(sourceNoteId)
        if (links.isNotEmpty()) {
            noteLinkDao.insertLinks(links.map { it.copy(id = 0, sourceNoteId = sourceNoteId) })
        }
    }
}
