package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.TimerDao
import com.apagon.rhythm.data.model.Timer
import kotlinx.coroutines.flow.Flow
class TimerRepository constructor(private val dao: TimerDao) {
    fun getAllTimers(): Flow<List<Timer>> = dao.getAllTimers()
    suspend fun getTimerById(id: Long): Timer? = dao.getById(id)
    suspend fun addTimer(timer: Timer): Long = dao.insert(timer)
    suspend fun updateTimer(timer: Timer) = dao.update(timer.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteTimer(timer: Timer) {
        val now = System.currentTimeMillis()
        dao.update(timer.copy(deletedAt = now, updatedAt = now))
    }
    suspend fun hardDeleteTimer(timer: Timer) = dao.delete(timer)
    fun getDeletedTimers(): Flow<List<Timer>> = dao.getDeletedTimers()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        dao.purgeDeletedTimers(olderThan)
    }

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    suspend fun getTimerBySyncId(syncId: String): Timer? = dao.getTimerBySyncId(syncId)

    suspend fun getTimersUpdatedSince(since: Long): List<Timer> = dao.getTimersUpdatedSince(since)

    suspend fun insertTimerFromSync(timer: Timer): Long = dao.insert(timer)

    suspend fun updateTimerFromSync(timer: Timer) = dao.update(timer)

    /**
     * Pomodoro phase state machine — transcribed from androidMain's
     * TimerCompletionReceiver.nextPomoState (Stage 12). Only ever
     * user-triggered ("tap Done to start break/work"), never automatic —
     * matches Android's own design, where the alert's action is what
     * advances the phase, not the completion itself.
     */
    suspend fun advancePomoPhase(timerId: Long) {
        val timer = dao.getById(timerId) ?: return
        val (nextPhase, nextSession) = when (timer.pomoPhase) {
            "WORK" -> if (timer.pomoCurrentSession >= timer.pomoSessionsPerRound) {
                "LONG_BREAK" to timer.pomoCurrentSession
            } else {
                "SHORT_BREAK" to timer.pomoCurrentSession
            }
            "SHORT_BREAK" -> "WORK" to timer.pomoCurrentSession + 1
            "LONG_BREAK" -> "WORK" to 1
            else -> "WORK" to 1
        }
        val nextDuration = when (nextPhase) {
            "WORK" -> timer.pomoWorkSecs
            "SHORT_BREAK" -> timer.pomoShortBreakSecs
            else -> timer.pomoLongBreakSecs
        }
        dao.update(
            timer.copy(
                durationSeconds = nextDuration,
                remainingSeconds = nextDuration,
                endTimeMillis = System.currentTimeMillis() + nextDuration * 1000L,
                pomoCurrentSession = nextSession,
                pomoPhase = nextPhase
            )
        )
    }
}
