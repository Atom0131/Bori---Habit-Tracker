package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.ReminderDao
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.isRepeating
import com.apagon.rhythm.data.model.nextRepeat
import com.apagon.rhythm.core.time.now
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
class ReminderRepository constructor(private val dao: ReminderDao) {

    fun getAllActiveReminders(): Flow<List<Reminder>> = dao.getAllActiveReminders()

    fun getCompletedRemindersForDay(dayStart: Long, dayEnd: Long): Flow<List<Reminder>> =
        dao.getCompletedRemindersForDay(dayStart, dayEnd)

    suspend fun addReminder(reminder: Reminder): Long = dao.insert(reminder)

    suspend fun updateReminder(reminder: Reminder) {
        val now = System.currentTimeMillis()
        // Ticking a repeating reminder done moves it to its next date (see completeRepeating).
        if (reminder.isCompleted && reminder.isRepeating && dao.getReminderByIdRaw(reminder.id)?.isCompleted == false &&
            completeRepeating(reminder, now)) return
        dao.update(reminder.copy(updatedAt = now))
    }

    /**
     * A repeating reminder marked done: a completed copy for today's done list, and the reminder itself
     * open again on its next date. False when it doesn't repeat or the series has ended. Same on Android.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun completeRepeating(reminder: Reminder, now: Long): Boolean {
        val next = reminder.copy(isCompleted = false).nextRepeat(LocalDateTime.now()) ?: return false
        dao.insert(reminder.copy(id = 0, syncId = Uuid.random().toString(), recurrence = "", isCompleted = true,
            completedAt = now, createdAt = now, updatedAt = now))
        dao.update(next.copy(updatedAt = now))
        return true
    }

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
    suspend fun getReminderTwins(createdAt: Long, title: String): List<Reminder> = dao.getReminderTwins(createdAt, title)

    suspend fun getRemindersUpdatedSince(since: Long): List<Reminder> = dao.getRemindersUpdatedSince(since)

    suspend fun insertReminderFromSync(reminder: Reminder): Long = dao.insert(reminder)

    suspend fun updateReminderFromSync(reminder: Reminder) = dao.update(reminder)
}
