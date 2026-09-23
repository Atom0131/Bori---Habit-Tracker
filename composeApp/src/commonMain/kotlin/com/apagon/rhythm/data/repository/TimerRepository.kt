package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.TimerDao
import com.apagon.rhythm.data.model.Timer
import kotlinx.coroutines.flow.Flow
class TimerRepository constructor(private val dao: TimerDao) {
    fun getAllTimers(): Flow<List<Timer>> = dao.getAllTimers()
    suspend fun getTimerById(id: Long): Timer? = dao.getById(id)
    suspend fun addTimer(timer: Timer): Long = dao.insert(timer)
    suspend fun updateTimer(timer: Timer) = dao.update(timer)
    suspend fun deleteTimer(timer: Timer) = dao.update(timer.copy(deletedAt = System.currentTimeMillis()))
    suspend fun hardDeleteTimer(timer: Timer) = dao.delete(timer)
    fun getDeletedTimers(): Flow<List<Timer>> = dao.getDeletedTimers()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        dao.purgeDeletedTimers(olderThan)
    }
}
