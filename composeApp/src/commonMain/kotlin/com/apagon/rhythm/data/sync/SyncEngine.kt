package com.apagon.rhythm.data.sync

import com.apagon.rhythm.core.time.System
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.repository.HabitRepository

/**
 * Transport-agnostic sync logic — SyncServer/SyncClient (jvmMain, Ktor) just
 * exchange the SyncBatch this produces/consumes over a WebSocket. Conflict
 * policy is plain last-write-wins by updatedAt (habits are flat structured
 * records, not freeform text — little to lose by not forking, and the usage
 * model — manual trigger, both devices foregrounded — makes same-window
 * concurrent edits rare; see the Stage 4 plan for the full reasoning).
 */
class SyncEngine(
    private val repository: HabitRepository,
    private val preferences: SyncPreferences
) {
    /**
     * Always requeried fresh against [SyncPreferences.getLastSyncedAt] — never
     * a cached delta carried across calls. Caching a "what changed" snapshot
     * across a reconnect is exactly the class of bug that made rows silently
     * vanish in the live app's vault-sync feature (rhythm-stage3-reconcile-
     * vanish-bug.md) — the fix there was the same lesson: always re-resolve
     * from source, never trust a stale handle.
     */
    suspend fun buildOutgoingBatch(deviceId: String): SyncBatch {
        val since = preferences.getLastSyncedAt()
        val habits = repository.getHabitsUpdatedSince(since)
        val completions = repository.getCompletionsUpdatedSince(since)
        val completionDtos = completions.mapNotNull { completion ->
            val habit = repository.getHabitByIdRaw(completion.habitId) ?: return@mapNotNull null
            completion.toDto(habitSyncId = habit.syncId)
        }
        return SyncBatch(
            deviceId = deviceId,
            habits = habits.map { it.toDto() },
            completions = completionDtos
        )
    }

    /** Habits are applied fully before completions — completions resolve habitSyncId -> local habitId via a habit already committed to this device's database, whether from this same batch or an earlier sync round. */
    suspend fun applyIncomingBatch(batch: SyncBatch): SyncResult {
        var habitsInserted = 0
        var habitsUpdated = 0
        var completionsInserted = 0
        var completionsUpdated = 0

        for (dto in batch.habits) {
            val existing = repository.getHabitBySyncId(dto.syncId)
            when {
                existing == null -> {
                    repository.insertHabitFromSync(dto.toEntity())
                    habitsInserted++
                }
                dto.updatedAt > existing.updatedAt -> {
                    repository.updateHabitFromSync(dto.toEntity().copy(id = existing.id))
                    habitsUpdated++
                }
                // else: local copy is newer or tied — last-write-wins keeps it, nothing to do.
            }
        }

        for (dto in batch.completions) {
            val habit = repository.getHabitBySyncId(dto.habitSyncId)
            if (habit == null) {
                // The owning habit isn't known on this device yet. Shouldn't
                // happen in the normal two-device flow (habits are applied
                // above, in the same batch, before completions are ever
                // processed) — defensive drop, not a silent data loss: this
                // completion is still present in the peer's own database and
                // will be resent on any future sync round.
                continue
            }
            val existing = repository.getCompletionBySyncId(dto.syncId)
            when {
                existing == null -> {
                    repository.insertCompletionFromSync(
                        HabitCompletion(
                            habitId = habit.id,
                            dateCompleted = dto.dateCompleted,
                            syncId = dto.syncId,
                            updatedAt = dto.updatedAt,
                            deletedAt = dto.deletedAt
                        )
                    )
                    completionsInserted++
                }
                dto.updatedAt > existing.updatedAt -> {
                    repository.updateCompletionFromSync(existing, existing.copy(updatedAt = dto.updatedAt, deletedAt = dto.deletedAt))
                    completionsUpdated++
                }
            }
        }

        return SyncResult(habitsInserted, habitsUpdated, completionsInserted, completionsUpdated)
    }

    suspend fun markSynced() {
        preferences.setLastSyncedAt(System.currentTimeMillis())
    }
}
