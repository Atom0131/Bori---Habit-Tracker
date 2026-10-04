package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.AlarmDao
import com.apagon.rhythm.data.model.Alarm
import kotlinx.coroutines.flow.Flow
class AlarmRepository constructor(private val dao: AlarmDao) {
    fun getAllAlarms(): Flow<List<Alarm>> = dao.getAllAlarms()
    suspend fun getEnabledAlarms(): List<Alarm> = dao.getEnabledAlarms()
    suspend fun addAlarm(alarm: Alarm): Long = dao.insert(alarm)
    suspend fun updateAlarm(alarm: Alarm) = dao.update(alarm.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteAlarm(alarm: Alarm) {
        val now = System.currentTimeMillis()
        dao.update(alarm.copy(deletedAt = now, updatedAt = now))
    }
    suspend fun hardDeleteAlarm(alarm: Alarm) = dao.delete(alarm)
    fun getDeletedAlarms(): Flow<List<Alarm>> = dao.getDeletedAlarms()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        dao.purgeDeletedAlarms(olderThan)
    }

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    suspend fun getAlarmBySyncId(syncId: String): Alarm? = dao.getAlarmBySyncId(syncId)

    suspend fun getAlarmsUpdatedSince(since: Long): List<Alarm> = dao.getAlarmsUpdatedSince(since)

    suspend fun insertAlarmFromSync(alarm: Alarm): Long = dao.insert(alarm)

    suspend fun updateAlarmFromSync(alarm: Alarm) = dao.update(alarm)
}
