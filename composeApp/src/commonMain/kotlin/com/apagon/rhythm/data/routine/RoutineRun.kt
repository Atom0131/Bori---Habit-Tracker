package com.apagon.rhythm.data.routine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Port of the phone's `data/routine/RoutineRun.kt` (Android 1.88). */
@Serializable
enum class RoutineStepStatus { PENDING, DONE, SKIPPED }

@Serializable
data class RoutineStep(
    val itemId: Long,
    val label: String,
    /** 0 = untimed: the step waits for Done instead of counting down. */
    val durationSeconds: Int,
    val status: RoutineStepStatus = RoutineStepStatus.PENDING
)

/**
 * One run of a routine, from Start to the summary. A snapshot of the steps is taken at Start so that
 * editing the habit mid-run cannot reshuffle the step the user is on.
 *
 * Time is held as **wall-clock instants, not a ticking counter**, so nothing has to run for the
 * countdown to stay right (a laptop asleep mid-step catches up when it wakes): the current step ends
 * at [stepEndMillis], and while paused the time left is frozen in [pausedRemainingMs] instead.
 */
@Serializable
data class RoutineRun(
    val habitId: Long,
    val habitName: String,
    /** The day the run ticks steps on (yyyy-MM-dd), fixed at Start so a run past midnight stays on its day. */
    val date: String,
    val steps: List<RoutineStep>,
    /** Index into [steps]; equal to `steps.size` once finished. */
    val currentIndex: Int,
    /** When the current step ends; 0 while paused or on an untimed step. */
    val stepEndMillis: Long,
    val isPaused: Boolean,
    val pausedRemainingMs: Long,
    val startedAt: Long,
    /** 0 while running; set when the last step is left, which is when the summary shows. */
    val finishedAt: Long
) {
    val isFinished: Boolean get() = finishedAt > 0
    val currentStep: RoutineStep? get() = if (isFinished) null else steps.getOrNull(currentIndex)

    /** Time left on the current step at [now], or null for an untimed step. */
    fun remainingMs(now: Long = System.currentTimeMillis()): Long? {
        val step = currentStep ?: return null
        if (step.durationSeconds <= 0) return null
        return if (isPaused) pausedRemainingMs else (stepEndMillis - now).coerceAtLeast(0)
    }

    /** Planned time still ahead: the current step's remainder plus every pending step after it. */
    fun remainingTotalMs(now: Long = System.currentTimeMillis()): Long {
        val current = remainingMs(now) ?: 0L
        val after = steps.drop(currentIndex + 1)
            .filter { it.status == RoutineStepStatus.PENDING }
            .sumOf { it.durationSeconds * 1000L }
        return current + after
    }

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Null for anything unreadable: a run that cannot be parsed is treated as no run at all. */
        fun fromJson(text: String?): RoutineRun? =
            if (text.isNullOrBlank()) null else runCatching { json.decodeFromString(serializer(), text) }.getOrNull()
    }
}
