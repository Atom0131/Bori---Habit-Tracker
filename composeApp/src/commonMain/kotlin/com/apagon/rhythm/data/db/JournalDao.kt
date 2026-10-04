package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.apagon.rhythm.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Query("SELECT * FROM journal_entries WHERE date = :date AND deletedAt IS NULL ORDER BY createdAt ASC")
    fun getEntriesForDate(date: String): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE habitId = :habitId AND deletedAt IS NULL ORDER BY date DESC")
    fun getEntriesForHabit(habitId: Long): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE date >= :startDate AND date <= :endDate AND deletedAt IS NULL ORDER BY date DESC, createdAt ASC")
    fun getEntriesInRange(startDate: String, endDate: String): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') AND deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    fun searchEntries(query: String): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedEntries(): Flow<List<JournalEntry>>

    @Query("DELETE FROM journal_entries WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeDeletedEntries(olderThan: Long)

    @Insert
    suspend fun insert(entry: JournalEntry): Long

    @Update
    suspend fun update(entry: JournalEntry)

    @Delete
    suspend fun delete(entry: JournalEntry)

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    @Query("SELECT * FROM journal_entries WHERE syncId = :syncId")
    suspend fun getEntryBySyncId(syncId: String): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE updatedAt > :since")
    suspend fun getEntriesUpdatedSince(since: Long): List<JournalEntry>

    @Query("SELECT * FROM journal_entries")
    suspend fun getAllForBackup(): List<JournalEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<JournalEntry>)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAll()
}
