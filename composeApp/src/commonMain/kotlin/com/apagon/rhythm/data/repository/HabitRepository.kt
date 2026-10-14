package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.ActiveDayDao
import com.apagon.rhythm.data.db.HabitDao
import com.apagon.rhythm.data.model.ActiveDay
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistItemCompletion
import com.apagon.rhythm.platform.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
class HabitRepository constructor(
    private val habitDao: HabitDao,
    private val widgetRefresher: WidgetRefresher,
    private val activeDayDao: ActiveDayDao
) {
    // ── Habits ────────────────────────────────────────────────────────────────

    fun getAllActiveHabits(): Flow<List<Habit>> = habitDao.getAllActiveHabits()

    fun getAllArchivedHabits(): Flow<List<Habit>> = habitDao.getAllArchivedHabits()

    fun getHabitById(id: Long): Flow<Habit?> = habitDao.getHabitById(id)

    suspend fun addHabit(habit: Habit): Long =
        habitDao.insertHabit(habit).also { widgetRefresher.refreshAll() }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit.copy(updatedAt = System.currentTimeMillis()))
        widgetRefresher.refreshAll()
    }

    /** Soft-delete: marks the habit inactive rather than removing it. */
    suspend fun archiveHabit(habit: Habit) {
        habitDao.updateHabit(habit.copy(isActive = false, updatedAt = System.currentTimeMillis()))
        widgetRefresher.refreshAll()
    }

    /** Restores an archived habit to the active list. */
    suspend fun restoreHabit(habit: Habit) {
        habitDao.updateHabit(habit.copy(isActive = true, updatedAt = System.currentTimeMillis()))
        widgetRefresher.refreshAll()
    }

    /** Soft-delete to "Recently Deleted": item stays in DB for 14 days. */
    suspend fun deleteHabit(habit: Habit) {
        val now = System.currentTimeMillis()
        habitDao.updateHabit(habit.copy(deletedAt = now, updatedAt = now))
        widgetRefresher.refreshAll()
    }

    // ── Sync (Stage 4) ───────────────────────────────────────────────────────
    // Applied from a remote peer's batch. These preserve the remote row's
    // updatedAt verbatim (it's the whole point of last-write-wins — a local
    // System.currentTimeMillis() stamp here would make every synced row look
    // newer than it is) but still route through this repository, not the DAO
    // directly, so widgetRefresher.refreshAll() still fires on the receiving
    // side exactly as it does for a locally-made change.

    suspend fun insertHabitFromSync(habit: Habit): Long =
        habitDao.insertHabit(habit).also { widgetRefresher.refreshAll() }

    suspend fun updateHabitFromSync(habit: Habit) {
        habitDao.updateHabit(habit)
        widgetRefresher.refreshAll()
    }

    suspend fun getHabitBySyncId(syncId: String): Habit? = habitDao.getHabitBySyncId(syncId)

    suspend fun getHabitByIdRaw(id: Long): Habit? = habitDao.getHabitByIdRaw(id)

    suspend fun getHabitsUpdatedSince(since: Long): List<Habit> = habitDao.getHabitsUpdatedSince(since)

    /** Permanently removes from DB. */
    suspend fun hardDeleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
        widgetRefresher.refreshAll()
    }

    fun getDeletedHabits(): Flow<List<Habit>> = habitDao.getDeletedHabits()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        habitDao.purgeDeletedHabits(olderThan)
    }

    suspend fun updateReminderTime(habitId: Long, time: String?) =
        habitDao.updateReminderTime(habitId, time)

    // ── Completions ───────────────────────────────────────────────────────────

    /**
     * Revive-or-insert: a previously-unchecked day left a tombstoned row
     * (see HabitCompletion.deletedAt) still occupying the unique (habitId,
     * dateCompleted) slot, so a plain @Insert(IGNORE) would silently no-op
     * on re-completing that day. Check for an existing row first — revive it
     * if found (tombstoned or not, revive is idempotent), otherwise insert.
     */
    suspend fun markComplete(habitId: Long, date: String): Long {
        val now = System.currentTimeMillis()
        val existing = habitDao.getCompletionRaw(habitId, date)
        return if (existing != null) {
            habitDao.setCompletionSyncState(habitId, date, deletedAt = null, updatedAt = now)
            widgetRefresher.refreshAll()
            existing.id
        } else {
            habitDao.insertCompletion(HabitCompletion(habitId = habitId, dateCompleted = date, updatedAt = now))
                .also { widgetRefresher.refreshAll() }
        }
    }

    /** Soft-delete so the "uncheck" propagates to a sync peer instead of vanishing. */
    suspend fun markIncomplete(habitId: Long, date: String) {
        val now = System.currentTimeMillis()
        habitDao.setCompletionSyncState(habitId, date, deletedAt = now, updatedAt = now)
        widgetRefresher.refreshAll()
    }

    suspend fun purgeOldDeletedCompletions(olderThan: Long) {
        habitDao.purgeDeletedCompletions(olderThan)
    }

    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> =
        habitDao.getCompletionsForHabit(habitId)

    fun getCompletionsByDate(date: String): Flow<List<HabitCompletion>> =
        habitDao.getCompletionsByDate(date)

    fun getCompletionsBetweenDates(startDate: String, endDate: String): Flow<List<HabitCompletion>> =
        habitDao.getCompletionsBetweenDates(startDate, endDate)

    // ── Active days (the never-resetting Today counter; filled by triggers) ───

    fun countActiveDaysThrough(today: String): Flow<Int> = activeDayDao.countThrough(today)

    suspend fun getAllActiveDays(): List<String> = activeDayDao.getAll()

    /** Union-merge days from a sync peer; a day already here is ignored. */
    suspend fun mergeActiveDays(days: List<String>) = activeDayDao.insertAll(days.map { ActiveDay(it) })

    // ── Sync (Stage 4) ───────────────────────────────────────────────────────

    suspend fun insertCompletionFromSync(completion: HabitCompletion): Long =
        habitDao.insertCompletion(completion).also { widgetRefresher.refreshAll() }

    /** Overwrites the existing row's deletedAt/updatedAt with the incoming (winning) row's state in one write. */
    suspend fun updateCompletionFromSync(existing: HabitCompletion, incoming: HabitCompletion) {
        habitDao.setCompletionSyncState(existing.habitId, existing.dateCompleted, incoming.deletedAt, incoming.updatedAt)
        widgetRefresher.refreshAll()
    }

    suspend fun getCompletionBySyncId(syncId: String): HabitCompletion? = habitDao.getCompletionBySyncId(syncId)

    suspend fun getCompletionsUpdatedSince(since: Long): List<HabitCompletion> = habitDao.getCompletionsUpdatedSince(since)

    // ── Checklist Items ───────────────────────────────────────────────────────

    suspend fun addHabitWithItems(habit: Habit, items: List<ChecklistDraft>): Long {
        val habitId = habitDao.insertHabit(habit)
        if (items.isNotEmpty()) {
            habitDao.insertChecklistItems(items.mapIndexed { index, draft ->
                ChecklistItem(
                    habitId = habitId, label = draft.label, sortOrder = index,
                    durationSeconds = draft.durationSeconds
                )
            })
        }
        widgetRefresher.refreshAll()
        return habitId
    }

    suspend fun updateHabitWithItems(habit: Habit, newItems: List<ChecklistDraft>) {
        habitDao.updateHabit(habit.copy(updatedAt = System.currentTimeMillis()))
        val existing = habitDao.getItemsForHabit(habit.id).first()

        // Rows keep their item (and so today's tick) by matching an existing item of the same
        // label **one to one, in order**. Matching by label alone gave two steps called "Rest" the
        // same item and dropped the second. The step's time comes from the editor, as on the phone.
        val unmatched = existing.sortedBy { it.sortOrder }.toMutableList()
        val toUpdate = mutableListOf<ChecklistItem>()
        val toInsert = mutableListOf<ChecklistItem>()
        newItems.forEachIndexed { index, draft ->
            val match = unmatched.firstOrNull { it.label == draft.label }
            if (match != null) {
                unmatched.remove(match)
                toUpdate += match.copy(sortOrder = index, durationSeconds = draft.durationSeconds)
            } else {
                toInsert += ChecklistItem(
                    habitId = habit.id, label = draft.label, sortOrder = index,
                    durationSeconds = draft.durationSeconds
                )
            }
        }
        val toDelete = unmatched.toList()

        if (toDelete.isNotEmpty()) {
            habitDao.deleteChecklistItems(toDelete)
        }
        if (toUpdate.isNotEmpty()) {
            habitDao.updateChecklistItems(toUpdate)
        }
        if (toInsert.isNotEmpty()) {
            habitDao.insertChecklistItems(toInsert)
        }
        widgetRefresher.refreshAll()
    }

    fun getItemsForHabit(habitId: Long): Flow<List<ChecklistItem>> =
        habitDao.getItemsForHabit(habitId)

    /** Ids of the habits that are timed routines (a step with a time). */
    fun getRoutineHabitIds(): Flow<List<Long>> = habitDao.getRoutineHabitIds()

    // ── Sync (Stage 2) — checklist items/completions, full-replace-per-parent ───────────────────
    // Nested two levels (Habit -> ChecklistItem -> ChecklistItemCompletion); itemId resolution
    // across the two levels is done by the caller (SyncEngine), same spirit as TodoRepository's
    // subtask primitives.

    suspend fun getItemsForHabitSync(habitId: Long): List<ChecklistItem> = habitDao.getItemsForHabitOnce(habitId)

    suspend fun deleteAllItemsForHabitSync(habitId: Long) = habitDao.deleteAllItemsForHabit(habitId)

    suspend fun insertChecklistItemFromSync(item: ChecklistItem): Long = habitDao.insertChecklistItem(item)

    suspend fun getCompletionsForItemSync(itemId: Long): List<ChecklistItemCompletion> =
        habitDao.getCompletionsForItemOnce(itemId)

    suspend fun insertItemCompletionFromSync(completion: ChecklistItemCompletion): Long =
        habitDao.insertItemCompletion(completion)

    // ── Checklist Item Completions ────────────────────────────────────────────

    suspend fun checkItem(itemId: Long, date: String): Long {
        val id = habitDao.insertItemCompletion(ChecklistItemCompletion(itemId = itemId, dateCompleted = date))
        habitDao.touchHabitForChecklistItem(itemId, System.currentTimeMillis())
        widgetRefresher.refreshAll()
        return id
    }

    suspend fun uncheckItem(itemId: Long, date: String) {
        habitDao.deleteItemCompletion(itemId, date)
        habitDao.touchHabitForChecklistItem(itemId, System.currentTimeMillis())
        widgetRefresher.refreshAll()
    }

    fun getItemCompletionsByHabitsOnDate(habitIds: List<Long>, date: String): Flow<List<ChecklistItemCompletion>> =
        habitDao.getItemCompletionsByHabitsOnDate(habitIds, date)

    /**
     * Toggles a checklist item and auto-completes or un-completes the parent habit.
     *
     * Extracted here to avoid duplicating the same logic across multiple ViewModels.
     * Each caller is responsible for passing its own context-specific state.
     *
     * @param habitId          The habit that owns the item.
     * @param item             The checklist item being toggled.
     * @param isCurrentlyDone  Whether the item is already checked on [date].
     * @param date             ISO_LOCAL_DATE string for the target date.
     * @param habitCompletedIds Set of habitIds already marked complete on [date].
     * @param allItemsForHabit All checklist items belonging to [habitId].
     * @param checkedItemIds   Set of itemIds already checked on [date].
     */
    suspend fun toggleChecklistItem(
        habitId: Long,
        item: ChecklistItem,
        isCurrentlyDone: Boolean,
        date: String,
        habitCompletedIds: Set<Long>,
        allItemsForHabit: List<ChecklistItem>,
        checkedItemIds: Set<Long>
    ) {
        if (isCurrentlyDone) {
            uncheckItem(item.id, date)
            if (habitId in habitCompletedIds) markIncomplete(habitId, date)
        } else {
            checkItem(item.id, date)
            // Query DB directly to prevent race conditions during rapid tapping
            val currentCompletions = habitDao.getItemCompletionsByHabitsOnDateSync(listOf(habitId), date)
            val currentCheckedIds = currentCompletions.map { it.itemId }.toSet() + item.id
            val allChecked = allItemsForHabit.all { it.id in currentCheckedIds }
            if (allChecked) markComplete(habitId, date)
        }
    }

    /**
     * A routine step finished: tick it on [date] if it isn't already, which completes the habit
     * by the ordinary checklist rule once the last step is ticked. Never unticks.
     */
    suspend fun completeRoutineStep(habitId: Long, itemId: Long, date: String) {
        val items = habitDao.getItemsForHabit(habitId).first()
        val item = items.firstOrNull { it.id == itemId } ?: return
        val checked = habitDao.getItemCompletionsByHabitsOnDateSync(listOf(habitId), date)
            .map { it.itemId }.toSet()
        if (item.id in checked) return
        toggleChecklistItem(habitId, item, isCurrentlyDone = false, date, emptySet(), items, checked)
    }
}

/** One row of the checklist editor: what the user typed, and the step's time (0 = untimed). */
data class ChecklistDraft(val label: String, val durationSeconds: Int = 0)
