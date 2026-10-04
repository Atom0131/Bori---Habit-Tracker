package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.JournalDao
import com.apagon.rhythm.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow
class JournalRepository constructor(private val dao: JournalDao) {

    fun getEntriesForDate(date: String): Flow<List<JournalEntry>> = dao.getEntriesForDate(date)

    fun getEntriesForHabit(habitId: Long): Flow<List<JournalEntry>> = dao.getEntriesForHabit(habitId)

    fun getEntriesInRange(startDate: String, endDate: String): Flow<List<JournalEntry>> =
        dao.getEntriesInRange(startDate, endDate)

    fun searchEntries(query: String): Flow<List<JournalEntry>> = dao.searchEntries(query)

    suspend fun addEntry(entry: JournalEntry): Long = dao.insert(entry)

    suspend fun updateEntry(entry: JournalEntry) = dao.update(entry.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteEntry(entry: JournalEntry) {
        val now = System.currentTimeMillis()
        dao.update(entry.copy(deletedAt = now, updatedAt = now))
    }

    suspend fun hardDeleteEntry(entry: JournalEntry) = dao.delete(entry)

    fun getDeletedEntries(): Flow<List<JournalEntry>> = dao.getDeletedEntries()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        dao.purgeDeletedEntries(olderThan)
    }

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    suspend fun getEntryBySyncId(syncId: String): JournalEntry? = dao.getEntryBySyncId(syncId)

    suspend fun getEntriesUpdatedSince(since: Long): List<JournalEntry> = dao.getEntriesUpdatedSince(since)

    suspend fun insertEntryFromSync(entry: JournalEntry): Long = dao.insert(entry)

    suspend fun updateEntryFromSync(entry: JournalEntry) = dao.update(entry)
}
