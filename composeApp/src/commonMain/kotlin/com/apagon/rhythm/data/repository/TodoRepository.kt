package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.TodoDao
import com.apagon.rhythm.data.db.TodoSubtaskDao
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.TodoSubtask
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
        dao.markCompleteById(todoId, now, now)
    }

    suspend fun markTodoComplete(todo: Todo) {
        val now = System.currentTimeMillis()
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
