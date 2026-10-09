package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.apagon.rhythm.data.model.TodoSubtask

// A separate @Dao from TodoDao (rather than methods added there) on purpose: TodoDao is also
// attached to commonMain's HabitDatabase (dead code at runtime, but still KSP-compiled as part
// of this module), which does not declare TodoSubtask in its entities list — a
// TodoSubtask-returning method on TodoDao would fail KSP validation for that unrelated
// @Database. Registered only on DesktopHabitDatabase.
@Dao
interface TodoSubtaskDao {
    @Query("SELECT * FROM todo_subtasks")
    suspend fun getAllSubtasksForBackup(): List<TodoSubtask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSubtasks(subtasks: List<TodoSubtask>)

    @Query("DELETE FROM todo_subtasks")
    suspend fun deleteAllSubtasks()

    // ── Sync (Stage 2) — full-replace-per-parent ────────────────────────────

    @Query("SELECT * FROM todo_subtasks WHERE todoId = :todoId ORDER BY sortOrder ASC")
    suspend fun getSubtasksForTodoOnce(todoId: Long): List<TodoSubtask>

    @Query("DELETE FROM todo_subtasks WHERE todoId = :todoId")
    suspend fun deleteSubtasksForTodo(todoId: Long)

    @Insert
    suspend fun insertSubtask(subtask: TodoSubtask): Long

    // Every mutator below ends by touching the parent to-do: subtasks only sync as part of their
    // to-do's full child list, sent only when the to-do itself qualifies via getTodosUpdatedSince.
    // Without the touch, an edit to a step would never leave this device.
    @Query("UPDATE todos SET updatedAt = :updatedAt WHERE id = :todoId")
    suspend fun touchTodo(todoId: Long, updatedAt: Long)

    // ── To-do checklists (ported from Android's TodoDao/TodoRepository, 2026-10-09) ──────────

    /** Steps of every live to-do, in document order, for the planner rows. */
    @Query(
        """
        SELECT s.* FROM todo_subtasks s
        JOIN todos t ON t.id = s.todoId
        WHERE t.deletedAt IS NULL
        ORDER BY s.todoId ASC, s.sortOrder ASC
        """
    )
    fun getSubtasksForLiveTodos(): Flow<List<TodoSubtask>>

    @Update
    suspend fun updateSubtask(subtask: TodoSubtask)

    @Query("DELETE FROM todo_subtasks WHERE id IN (:ids)")
    suspend fun deleteSubtasksByIds(ids: List<Long>)

    @Query("SELECT id FROM todo_subtasks WHERE parentId IN (:parentIds)")
    suspend fun getChildIds(parentIds: List<Long>): List<Long>

    @Query("UPDATE todo_subtasks SET isDone = :done, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setSubtaskDone(id: Long, done: Boolean, updatedAt: Long)

    @Query("UPDATE todo_subtasks SET isDone = :done, updatedAt = :updatedAt WHERE todoId = :todoId")
    suspend fun setAllSubtasksDone(todoId: Long, done: Boolean, updatedAt: Long)

    @Query("UPDATE todo_subtasks SET isDone = :done, updatedAt = :updatedAt WHERE parentId = :parentId")
    suspend fun setChildrenDone(parentId: Long, done: Boolean, updatedAt: Long)

    @Query("UPDATE todo_subtasks SET isDone = 0, updatedAt = :updatedAt WHERE id = :id AND isDone = 1")
    suspend fun reopenSubtask(id: Long, updatedAt: Long)

    /** Ticks a group once all its children are ticked. The EXISTS stops a childless row from
     * counting as "no open children" and ticking itself. */
    @Query(
        """
        UPDATE todo_subtasks SET isDone = 1, updatedAt = :updatedAt
        WHERE id = :parentId AND isDone = 0
          AND EXISTS (SELECT 1 FROM todo_subtasks WHERE parentId = :parentId)
          AND NOT EXISTS (SELECT 1 FROM todo_subtasks WHERE parentId = :parentId AND isDone = 0)
        """
    )
    suspend fun completeParentIfAllChildrenDone(parentId: Long, updatedAt: Long): Int

    /** Completes a to-do once every step is ticked (and it has steps at all). */
    @Query(
        """
        UPDATE todos SET isCompleted = 1, completedAt = :completedAt, updatedAt = :completedAt
        WHERE id = :todoId AND isCompleted = 0
          AND EXISTS (SELECT 1 FROM todo_subtasks WHERE todoId = :todoId)
          AND NOT EXISTS (SELECT 1 FROM todo_subtasks WHERE todoId = :todoId AND isDone = 0)
        """
    )
    suspend fun completeIfAllSubtasksDone(todoId: Long, completedAt: Long): Int

    @Query("UPDATE todos SET isCompleted = 0, completedAt = NULL, updatedAt = :updatedAt WHERE id = :id AND isCompleted = 1")
    suspend fun reopenTodoById(id: Long, updatedAt: Long)

    /**
     * Android's `TodoRepository.toggleSubtask`. A group's tick carries its children; a child's tick
     * can complete or reopen its group; and the last step ticked completes the to-do, any step
     * unticked reopens it. Returns true when this tick completed the to-do.
     */
    @Transaction
    suspend fun toggleSubtask(subtask: TodoSubtask, now: Long): Boolean {
        val nowDone = !subtask.isDone
        setSubtaskDone(subtask.id, nowDone, now)
        val parentId = subtask.parentId
        if (parentId == null) setChildrenDone(subtask.id, nowDone, now)
        else if (nowDone) completeParentIfAllChildrenDone(parentId, now)
        else reopenSubtask(parentId, now)
        val autoCompleted = if (nowDone) completeIfAllSubtasksDone(subtask.todoId, now) > 0 else {
            reopenTodoById(subtask.todoId, now)
            false
        }
        // Fixes the case Android misses: a tick that neither completes nor reopens the to-do
        // still has to mark it changed, or it never syncs.
        touchTodo(subtask.todoId, now)
        return autoCompleted
    }

    /**
     * Android's `TodoRepository.replaceSubtasks`: applies an editor's flat list, diffing by id
     * (never by label, so fixing a typo keeps a tick). A draft's parentId is only a child flag;
     * the real parent is the nearest preceding top-level row. Then re-applies the completion rule.
     */
    @Transaction
    suspend fun replaceSubtasks(todoId: Long, drafts: List<TodoSubtask>, now: Long) {
        val existing = getSubtasksForTodoOnce(todoId)
        val keptIds = drafts.mapNotNull { it.id.takeIf { id -> id != 0L } }.toSet()
        val removed = existing.filter { it.id !in keptIds }.map { it.id }
        if (removed.isNotEmpty()) {
            val orphans = getChildIds(removed).filter { it !in keptIds }
            deleteSubtasksByIds(removed + orphans)
        }
        val topLevelIdByIndex = mutableMapOf<Int, Long>()
        drafts.forEachIndexed { index, draft ->
            if (draft.parentId != null) return@forEachIndexed
            val row = draft.copy(todoId = todoId, sortOrder = index, parentId = null, updatedAt = now)
            topLevelIdByIndex[index] = if (row.id == 0L) insertSubtask(row) else { updateSubtask(row); row.id }
        }
        drafts.forEachIndexed { index, draft ->
            if (draft.parentId == null) return@forEachIndexed
            val parentId = topLevelIdByIndex.filterKeys { it < index }.maxByOrNull { it.key }?.value
            val row = draft.copy(todoId = todoId, sortOrder = index, parentId = parentId, updatedAt = now)
            if (row.id == 0L) insertSubtask(row) else updateSubtask(row)
        }
        if (drafts.isNotEmpty()) {
            if (getSubtasksForTodoOnce(todoId).any { !it.isDone }) reopenTodoById(todoId, now)
            else completeIfAllSubtasksDone(todoId, now)
        }
        touchTodo(todoId, now)
    }
}
