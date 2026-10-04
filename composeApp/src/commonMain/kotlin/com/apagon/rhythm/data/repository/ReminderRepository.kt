package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.ReminderDao
import com.apagon.rhythm.data.model.Reminder
import kotlinx.coroutines.flow.Flow
class ReminderRepository constructor(private val dao: ReminderDao) {

    fun getAllActiveReminders(): Flow<List<Reminder>> = dao.getAllActiveReminders()

    fun getCompletedRemindersForDay(dayStart: Long, dayEnd: Long): Flow<List<Reminder>> =
        dao.getCompletedRemindersForDay(dayStart, dayEnd)

    suspend fun addReminder(reminder: Reminder): Long = dao.insert(reminder)

    suspend fun updateReminder(reminder: Reminder) = dao.update(reminder.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteReminder(reminder: Reminder) {
        val now = System.currentTimeMillis()
        dao.update(reminder.copy(deletedAt = now, updatedAt = now))
    }

    suspend fun hardDeleteReminder(reminder: Reminder) = dao.delete(reminder)

    fun getDeletedReminders(): Flow<List<Reminder>> = dao.getDeletedReminders()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        dao.purgeDeletedReminders(olderThan)
    }

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    suspend fun getReminderBySyncId(syncId: String): Reminder? = dao.getReminderBySyncId(syncId)

    suspend fun getRemindersUpdatedSince(since: Long): List<Reminder> = dao.getRemindersUpdatedSince(since)

    suspend fun insertReminderFromSync(reminder: Reminder): Long = dao.insert(reminder)

    suspend fun updateReminderFromSync(reminder: Reminder) = dao.update(reminder)
}
