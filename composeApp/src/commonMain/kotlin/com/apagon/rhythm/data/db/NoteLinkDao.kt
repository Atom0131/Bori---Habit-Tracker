package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.apagon.rhythm.data.model.NoteLink

// Backup/restore only, this pass — desktop's note editor doesn't generate links yet.
//
// A separate @Dao from NotesDao (rather than methods added there) on purpose: NotesDao is also
// attached to commonMain's HabitDatabase (dead code at runtime, but still KSP-compiled as part
// of this module), which does not declare NoteLink in its entities list — a NoteLink-returning
// method on NotesDao would fail KSP validation for that unrelated @Database. Registered only on
// DesktopHabitDatabase.
@Dao
interface NoteLinkDao {
    @Query("SELECT * FROM note_links")
    suspend fun getAllNoteLinksForBackup(): List<NoteLink>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNoteLinks(links: List<NoteLink>)

    @Query("DELETE FROM note_links")
    suspend fun deleteAllNoteLinks()

    // ── Sync (Stage 2) — full-replace-per-parent ────────────────────────────

    @Query("SELECT * FROM note_links WHERE sourceNoteId = :sourceNoteId")
    suspend fun getLinksForSourceOnce(sourceNoteId: Long): List<NoteLink>

    @Query("DELETE FROM note_links WHERE sourceNoteId = :sourceNoteId")
    suspend fun deleteLinksForSource(sourceNoteId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<NoteLink>)

    // Desktop's note editor doesn't generate links yet (see the top-of-file comment). The moment
    // one is built, its mutator must call touchNote after mutating a link row, or the edit will
    // silently never sync — links only sync as part of their source note's full current link list,
    // sent only when the parent note itself qualifies via getNonPrivateNotesUpdatedSince.
    @Query("UPDATE notes SET updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun touchNote(noteId: Long, updatedAt: Long)
}
