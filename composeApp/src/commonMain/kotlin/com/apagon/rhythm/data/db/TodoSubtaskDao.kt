package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.apagon.rhythm.data.model.TodoSubtask

// Backup/restore only, this pass — desktop's to-do editor doesn't build nested checklists yet.
//
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

    // No subtask editor UI exists on desktop yet (see the top-of-file comment). The moment one is
    // built, its mutator must call touchTodo (or an equivalent parent-touch) after mutating a
    // subtask row, or the edit will silently never sync — subtasks only sync as part of their
    // parent todo's full current child list, sent only when the parent itself qualifies via
    // getTodosUpdatedSince.
    @Query("UPDATE todos SET updatedAt = :updatedAt WHERE id = :todoId")
    suspend fun touchTodo(todoId: Long, updatedAt: Long)
}
