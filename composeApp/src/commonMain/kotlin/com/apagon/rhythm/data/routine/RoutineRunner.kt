package com.apagon.rhythm.data.routine

import com.apagon.rhythm.core.time.now
import com.apagon.rhythm.data.db.RoutineRunLogDao
import com.apagon.rhythm.data.model.RoutineRunLog
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.repository.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate

/** What the runner needs from the computer it runs on. Implemented by `DesktopRoutinePlatform`. */
interface RoutinePlatform {
    /** The saved run, so a routine survives Bori being closed or the computer restarting. */
    fun loadRun(): String?
    fun saveRun(json: String?)

    /** One short chime between steps. */
    fun chime()

    /** Reads [text] aloud, if the system has a voice. */
    fun speak(text: String)

    /** Null when steps can be read aloud here; otherwise what to install, for the player to say. */
    fun missingVoiceHint(): String?

    /** A system notification, so a step change is seen with Bori in the background. */
    fun notify(title: String, text: String)
}

/**
 * Port of the phone's `RoutineRunner` (Android 1.88): the one owner of a running routine. The
 * player, the sidebar card and the timer all read and change it through here, so they cannot
 * disagree about which step is current.
 *
 * The phone wakes itself with an exact alarm for each step's end. The desktop has no such thing
 * and doesn't need it: a coroutine checks twice a second while a timed step runs, and because the
 * run holds wall-clock instants, a computer that slept through several steps catches up on waking
 * (each next step starts when the previous one *ended*, so the routine doesn't stretch).
 *
 * Steps are ticked on the habit as they finish (through [HabitRepository.completeRoutineStep]), so
 * the habit completes by the ordinary checklist rule the moment its last step is done. A skipped
 * step stays unticked.
 */
class RoutineRunner(
    private val habits: HabitRepository,
    private val prefs: ThemePreferences,
    private val history: RoutineRunLogDao,
    private val platform: RoutinePlatform
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val _run = MutableStateFlow(RoutineRun.fromJson(platform.loadRun()))
    val run: StateFlow<RoutineRun?> = _run

    init {
        scope.launch {
            clearStaleSummary()
            var tick = 0
            while (true) {
                val r = _run.value
                if (r != null && !r.isFinished && !r.isPaused && r.stepEndMillis > 0) {
                    if (System.currentTimeMillis() >= r.stepEndMillis - 500) onTimeUp()
                }
                // A habit deleted or archived mid-run (here, or by a sync) ends its routine, as on the
                // phone, instead of leaving a timer running for something no longer in the list.
                if (r != null && !r.isFinished && tick++ % 10 == 0) {
                    val habit = runCatching { habits.getHabitByIdRaw(r.habitId) }.getOrNull()
                    if (habit == null || !habit.isActive || habit.deletedAt != null) endIfHabit(r.habitId)
                }
                delay(500)
            }
        }
    }

    /** The last few finished runs of [habitId], newest first, for the player's history. */
    fun recentRuns(habitId: Long, limit: Int = 7): Flow<List<RoutineRunLog>> = history.recentForHabit(habitId, limit)

    fun current(): RoutineRun? = _run.value

    /** See [RoutinePlatform.missingVoiceHint]. */
    val missingVoiceHint: String? by lazy { runCatching { platform.missingVoiceHint() }.getOrNull() }

    /**
     * Starts [habitId]'s routine, replacing any other run. Steps already ticked today are carried in
     * as done, so starting a half-finished routine picks up where it was left.
     */
    suspend fun start(habitId: Long) {
        val entered = mutex.withLock {
            val habit = habits.getHabitByIdRaw(habitId) ?: return
            val items = habits.getItemsForHabit(habitId).first().sortedBy { it.sortOrder }
            if (items.isEmpty()) return
            val today = LocalDate.now().toString()
            val ticked = habits.getItemCompletionsByHabitsOnDate(listOf(habitId), today).first()
                .map { it.itemId }.toSet()
            // A routine already finished today runs again from the top; ticking is a no-op then.
            val allTicked = items.all { it.id in ticked }
            val steps = items.map {
                RoutineStep(
                    itemId = it.id, label = it.label, durationSeconds = it.durationSeconds,
                    status = if (!allTicked && it.id in ticked) RoutineStepStatus.DONE else RoutineStepStatus.PENDING
                )
            }
            val now = System.currentTimeMillis()
            val fresh = RoutineRun(
                habitId = habitId, habitName = habit.name, date = today, steps = steps,
                currentIndex = -1, stepEndMillis = 0, isPaused = false, pausedRemainingMs = 0,
                startedAt = now, finishedAt = 0
            )
            fresh.enter(fresh.nextPending(-1), now).also { commit(it) }
        }
        announce(entered, afterChime = false)
    }

    suspend fun pause() = update { run, now ->
        val remaining = run.remainingMs(now)
        if (run.isPaused || remaining == null) run
        else run.copy(isPaused = true, pausedRemainingMs = remaining, stepEndMillis = 0)
    }

    suspend fun resume() = update { run, now ->
        if (!run.isPaused) run
        else run.copy(isPaused = false, stepEndMillis = now + run.pausedRemainingMs, pausedRemainingMs = 0)
    }

    suspend fun addMinute() = update { run, _ ->
        when {
            run.currentStep?.durationSeconds?.let { it > 0 } != true -> run
            run.isPaused -> run.copy(pausedRemainingMs = run.pausedRemainingMs + 60_000)
            else -> run.copy(stepEndMillis = run.stepEndMillis + 60_000)
        }
    }

    /** The user says the current step is done, whatever its timer says. */
    suspend fun completeCurrent() = update { run, now -> finishStep(run, now, RoutineStepStatus.DONE, now) }

    suspend fun skipCurrent() = update { run, now -> finishStep(run, now, RoutineStepStatus.SKIPPED, now) }

    /**
     * Advances past every step whose time is up (more than one if the computer slept through
     * several) and chimes once. A paused or untimed step is never advanced from here.
     */
    suspend fun onTimeUp() {
        var advanced = false
        mutex.withLock {
            var run = current() ?: return
            val now = System.currentTimeMillis()
            while (true) {
                val step = run.currentStep ?: break
                if (run.isPaused || step.durationSeconds <= 0) break
                // Half a second of grace, so a check landing a hair early still counts.
                if (now < run.stepEndMillis - 500) break
                run = finishStep(run, now, RoutineStepStatus.DONE, run.stepEndMillis)
                advanced = true
            }
            if (advanced) commit(run)
        }
        if (advanced) {
            platform.chime()
            current()?.let {
                notifyStep(it)
                announce(it, afterChime = true)
            }
        }
    }

    /** Drops a finished run from an earlier day, so the routine opens on Start, not yesterday's summary. */
    suspend fun clearStaleSummary() = mutex.withLock {
        val run = current() ?: return@withLock
        if (run.isFinished && run.date != LocalDate.now().toString()) commit(null)
    }

    /** Summary: tick every step not yet done, which completes the habit. */
    suspend fun markAllDone() = update { run, now ->
        run.steps.filter { it.status != RoutineStepStatus.DONE }.forEach {
            habits.completeRoutineStep(run.habitId, it.itemId, run.date)
        }
        val steps = run.steps.map { it.copy(status = RoutineStepStatus.DONE) }
        run.copy(steps = steps, currentIndex = steps.size, finishedAt = run.finishedAt.takeIf { it > 0 } ?: now,
            stepEndMillis = 0, isPaused = false)
    }

    /** Ends the run if it belongs to [habitId]: for a habit being deleted or archived. */
    suspend fun endIfHabit(habitId: Long) = mutex.withLock {
        if (current()?.habitId == habitId) commit(null)
    }

    /** Ends the run, finished or not, and clears it. Ticked steps stay ticked. */
    suspend fun end() = mutex.withLock { commit(null) }

    // ── internals ────────────────────────────────────────────────────────────────────────────────

    private suspend fun update(block: suspend (RoutineRun, Long) -> RoutineRun) {
        val (before, after) = mutex.withLock {
            val run = current() ?: return
            val next = block(run, System.currentTimeMillis())
            commit(next)
            run to next
        }
        // Done / Skip / Mark-all moved to another step (or to the end): say what is next.
        if (after.currentIndex != before.currentIndex || after.isFinished != before.isFinished) {
            announce(after, afterChime = false)
        }
    }

    /** Leaves the current step with [status] and enters the next pending one at [nextStartsAt]. */
    private suspend fun finishStep(run: RoutineRun, now: Long, status: RoutineStepStatus, nextStartsAt: Long): RoutineRun {
        val step = run.currentStep ?: return run
        if (status == RoutineStepStatus.DONE) habits.completeRoutineStep(run.habitId, step.itemId, run.date)
        val steps = run.steps.toMutableList().also { it[run.currentIndex] = step.copy(status = status) }
        val marked = run.copy(steps = steps)
        return marked.enter(marked.nextPending(run.currentIndex), nextStartsAt.coerceAtMost(now))
    }

    private fun RoutineRun.nextPending(after: Int): Int =
        steps.indices.firstOrNull { it > after && steps[it].status == RoutineStepStatus.PENDING } ?: steps.size

    private fun RoutineRun.enter(index: Int, at: Long): RoutineRun =
        if (index >= steps.size) {
            copy(currentIndex = steps.size, stepEndMillis = 0, isPaused = false, pausedRemainingMs = 0,
                finishedAt = at.coerceAtLeast(startedAt))
        } else {
            val seconds = steps[index].durationSeconds
            copy(currentIndex = index, isPaused = false, pausedRemainingMs = 0,
                stepEndMillis = if (seconds > 0) at + seconds * 1000L else 0)
        }

    private suspend fun commit(run: RoutineRun?) {
        // Log every finished run (and re-log it if Mark-all-done changes its counts afterwards).
        if (run != null && run.isFinished) runCatching { history.upsert(run.toLog()) }
        _run.value = run
        runCatching { platform.saveRun(run?.toJson()) }
    }

    private fun RoutineRun.toLog() = RoutineRunLog(
        habitId = habitId,
        date = date,
        startedAt = startedAt,
        finishedAt = finishedAt,
        stepsDone = steps.count { it.status == RoutineStepStatus.DONE },
        stepsSkipped = steps.count { it.status == RoutineStepStatus.SKIPPED },
        stepsTotal = steps.size,
        plannedSeconds = steps.sumOf { it.durationSeconds }
    )

    /** The phone keeps an ongoing notification; the desktop says each change once, when the timer moves it. */
    private fun notifyStep(run: RoutineRun) {
        val text = if (run.isFinished) "Routine complete" else run.currentStep?.let { step ->
            if (step.durationSeconds > 0) "${step.label} · ${formatStepDuration(step.durationSeconds)}" else step.label
        } ?: return
        runCatching { platform.notify(run.habitName, text) }
    }

    /**
     * Reads the step just entered aloud ("Stretch, 5 minutes"), or "Routine complete", when spoken
     * steps are on. After a chime it waits a beat so the two don't talk over each other.
     */
    private suspend fun announce(run: RoutineRun, afterChime: Boolean) {
        if (!prefs.routineSpeakSteps.first()) return
        val text = when {
            run.isFinished -> "Routine complete"
            else -> run.currentStep?.let { step ->
                if (step.durationSeconds > 0) "${step.label}, ${spokenDuration(step.durationSeconds)}" else step.label
            }
        } ?: return
        scope.launch {
            if (afterChime) delay(900)
            runCatching { platform.speak(text) }
        }
    }

    private fun spokenDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return when {
            m == 0 -> "$s seconds"
            s == 0 -> if (m == 1) "1 minute" else "$m minutes"
            else -> "${if (m == 1) "1 minute" else "$m minutes"} $s seconds"
        }
    }
}

/** "45 s", "2 min", "1 min 30 s", "1 h 5 min": short enough for a chip. Same as the phone's. */
fun formatStepDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> if (m > 0) "$h h $m min" else "$h h"
        m > 0 -> if (s > 0) "$m min $s s" else "$m min"
        else -> "$s s"
    }
}

/** "4:05" or "1:02:09": a running countdown. */
fun formatCountdown(totalSeconds: Long): String {
    val t = totalSeconds.coerceAtLeast(0)
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}

/** A habit is a routine when it has steps and at least one is timed. */
fun List<com.apagon.rhythm.data.model.ChecklistItem>.isRoutine(): Boolean = any { it.durationSeconds > 0 }

/** Total planned time of a routine's timed steps, in seconds. */
fun List<com.apagon.rhythm.data.model.ChecklistItem>.routineTotalSeconds(): Int = sumOf { it.durationSeconds }
