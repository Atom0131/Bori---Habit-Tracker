package com.apagon.rhythm.data.db

import androidx.room.*
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.Notebook
import kotlinx.coroutines.flow.Flow

data class NoteCountResult(
    @ColumnInfo(name = "notebookId") val notebookId: Long,
    @ColumnInfo(name = "count") val count: Int
)

@Dao
interface NotesDao {

    // Notebooks
    @Query("SELECT * FROM notebooks WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getAllNotebooks(): Flow<List<Notebook>>

    @Query("SELECT * FROM notebooks WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedNotebooks(): Flow<List<Notebook>>

    @Query("DELETE FROM notebooks WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeDeletedNotebooks(olderThan: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: Notebook): Long

    @Update
    suspend fun updateNotebook(notebook: Notebook)

    @Delete
    suspend fun deleteNotebook(notebook: Notebook)

    @Query("SELECT * FROM notebooks WHERE id = :id")
    suspend fun getNotebookById(id: Long): Notebook?

    // Notes
    @Query("SELECT * FROM notes WHERE notebookId = :notebookId AND deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByNotebook(notebookId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<Note>>

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeDeletedNotes(olderThan: Long)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    fun searchAllNotes(query: String): Flow<List<Note>>

    @Query("SELECT notebookId, COUNT(*) as count FROM notes WHERE deletedAt IS NULL GROUP BY notebookId")
    fun getNoteCounts(): Flow<List<NoteCountResult>>

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────
    // Private notebooks/notes are excluded at the query itself, not filtered after the fact —
    // a privacy boundary, not an optimization. See SyncEngine's own defensive re-check on apply.

    @Query("SELECT * FROM notebooks WHERE syncId = :syncId")
    suspend fun getNotebookBySyncId(syncId: String): Notebook?

    @Query("SELECT * FROM notebooks WHERE updatedAt > :since AND isPrivate = 0")
    suspend fun getNonPrivateNotebooksUpdatedSince(since: Long): List<Notebook>

    @Query("SELECT * FROM notes WHERE syncId = :syncId")
    suspend fun getNoteBySyncId(syncId: String): Note?

    @Query("""
        SELECT notes.* FROM notes
        JOIN notebooks ON notebooks.id = notes.notebookId
        WHERE notes.updatedAt > :since AND notebooks.isPrivate = 0
    """)
    suspend fun getNonPrivateNotesUpdatedSince(since: Long): List<Note>

    // Backup/restore
    @Query("SELECT * FROM notebooks")
    suspend fun getAllNotebooksForBackup(): List<Notebook>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesForBackup(): List<Note>

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("DELETE FROM notebooks")
    suspend fun deleteAllNotebooks()
}
