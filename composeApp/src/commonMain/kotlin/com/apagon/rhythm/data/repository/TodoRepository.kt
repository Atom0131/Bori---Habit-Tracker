package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.TodoDao
import com.apagon.rhythm.data.db.TodoSubtaskDao
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.TodoSubtask
import com.apagon.rhythm.data.model.isRepeating
import com.apagon.rhythm.data.model.nextRepeat
import com.apagon.rhythm.core.time.now
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
class TodoRepository constructor(
    private val dao: TodoDao,
    private val subtaskDao: TodoSubtaskDao
) {

    fun getPendingTodos(): Flow<List<Todo>> = dao.getPendingTodos()

    fun getArchivedTodos(): Flow<List<Todo>> = dao.getArchivedTodos()

    fun getCompletedTodos(): Flow<List<Todo>> = dao.getCompletedTodos()

    fun getCompletedTodosForDay(dayStart: Long, dayEnd: Long): Flow<List<Todo>> =
        dao.getCompletedTodosForDay(dayStart, dayEnd)

    fun getAllActiveTodos(): Flow<List<Todo>> = dao.getAllActiveTodos()

    suspend fun addTodo(todo: Todo): Long = dao.insert(todo)

    suspend fun updateTodo(todo: Todo) = dao.update(todo.copy(updatedAt = System.currentTimeMillis()))

    suspend fun archiveTodo(todo: Todo, archive: Boolean = true) {
        dao.update(todo.copy(isArchived = archive, updatedAt = System.currentTimeMillis()))
    }

    suspend fun markComplete(todoId: Long) {
        val now = System.currentTimeMillis()
        if (dao.getTodoByIdRaw(todoId)?.let { rollIfRepeating(it, now) } == true) return
        dao.markCompleteById(todoId, now, now)
    }

    /**
     * **A repeating to-do never finishes; it moves to its next date** (Todoist-style). A completed copy
     * is saved so the check-off still shows in today's done list, and the to-do itself comes back open
     * on its next date with its steps unticked. Returns false (do a normal completion) when it doesn't
     * repeat or its series has ended. Android's TodoRepository does the same.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun rollIfRepeating(todo: Todo, now: Long): Boolean {
        if (!todo.isRepeating) return false
        val next = todo.nextRepeat(LocalDate.now()) ?: return false
        dao.insert(todo.copy(id = 0, syncId = Uuid.random().toString(), recurrence = "", isCompleted = true,
            completedAt = now, createdAt = now, updatedAt = now))
        dao.update(next.copy(updatedAt = now))
        subtaskDao.setAllSubtasksDone(todo.id, false, now)
        return true
    }

    /** Android's `setCompleted`: the to-do's own check carries its steps both ways (completing
     * ticks them all, reopening clears them, or the row would sit "open with every step done"). */
    suspend fun setCompleted(todo: Todo, completed: Boolean) {
        val now = System.currentTimeMillis()
        if (completed && rollIfRepeating(todo, now)) return
        dao.update(todo.copy(isCompleted = completed, completedAt = if (completed) now else null, updatedAt = now))
        subtaskDao.setAllSubtasksDone(todo.id, completed, now)
    }

    fun getSubtasksForLiveTodos(): Flow<List<TodoSubtask>> = subtaskDao.getSubtasksForLiveTodos()

    suspend fun getSubtasksForTodo(todoId: Long): List<TodoSubtask> = subtaskDao.getSubtasksForTodoOnce(todoId)

    /** True if ticking this step completed the to-do (a repeating one has moved on to its next date instead). */
    suspend fun toggleSubtask(subtask: TodoSubtask): Boolean {
        val now = System.currentTimeMillis()
        val completedTodo = subtaskDao.toggleSubtask(subtask, now)
        if (completedTodo) dao.getTodoByIdRaw(subtask.todoId)?.let { rollIfRepeating(it, now) }
        return completedTodo
    }

    suspend fun replaceSubtasks(todoId: Long, drafts: List<TodoSubtask>) =
        subtaskDao.replaceSubtasks(todoId, drafts, System.currentTimeMillis())

    suspend fun markTodoComplete(todo: Todo) {
        val now = System.currentTimeMillis()
        if (rollIfRepeating(todo, now)) return
        dao.update(todo.copy(isCompleted = true, completedAt = now, updatedAt = now))
    }

    suspend fun deleteTodo(todo: Todo): Unit {
        val now = System.currentTimeMillis()
        dao.update(todo.copy(deletedAt = now, updatedAt = now))
    }

    suspend fun hardDeleteTodo(todo: Todo) = dao.delete(todo)

    fun getDeletedTodos(): Flow<List<Todo>> = dao.getDeletedTodos()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        dao.purgeDeletedTodos(olderThan)
    }

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    suspend fun getTodoBySyncId(syncId: String): Todo? = dao.getTodoBySyncId(syncId)

    suspend fun getTodosUpdatedSince(since: Long): List<Todo> = dao.getTodosUpdatedSince(since)

    suspend fun insertTodoFromSync(todo: Todo): Long = dao.insert(todo)

    suspend fun updateTodoFromSync(todo: Todo) = dao.update(todo)

    // ── Sync (Stage 2) — subtasks, full-replace-per-parent ───────────────────
    // Parent-resolution (syncId -> local parentId, two-pass by depth) is done by the caller
    // (SyncEngine); these are plain primitives, same spirit as HabitRepository's checklist ones.

    suspend fun getSubtasksForTodoSync(todoId: Long): List<TodoSubtask> = subtaskDao.getSubtasksForTodoOnce(todoId)

    suspend fun deleteSubtasksForTodoSync(todoId: Long) = subtaskDao.deleteSubtasksForTodo(todoId)

    suspend fun insertSubtaskFromSync(subtask: TodoSubtask): Long = subtaskDao.insertSubtask(subtask)
}
