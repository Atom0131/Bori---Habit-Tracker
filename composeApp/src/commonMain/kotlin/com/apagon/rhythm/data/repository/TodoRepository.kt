package com.apagon.rhythm.data.repository

import com.apagon.rhythm.core.time.System

import com.apagon.rhythm.data.db.TodoDao
import com.apagon.rhythm.data.model.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
class TodoRepository constructor(private val dao: TodoDao) {

    fun getPendingTodos(): Flow<List<Todo>> = dao.getPendingTodos()

    fun getArchivedTodos(): Flow<List<Todo>> = dao.getArchivedTodos()

    fun getCompletedTodos(): Flow<List<Todo>> = dao.getCompletedTodos()

    fun getCompletedTodosForDay(dayStart: Long, dayEnd: Long): Flow<List<Todo>> =
        dao.getCompletedTodosForDay(dayStart, dayEnd)

    fun getAllActiveTodos(): Flow<List<Todo>> = dao.getAllActiveTodos()

    suspend fun addTodo(todo: Todo): Long = dao.insert(todo)

    suspend fun updateTodo(todo: Todo) = dao.update(todo)

    suspend fun archiveTodo(todo: Todo, archive: Boolean = true) {
        dao.update(todo.copy(isArchived = archive))
    }

    suspend fun markComplete(todoId: Long) {
        dao.markCompleteById(todoId, System.currentTimeMillis())
    }

    suspend fun markTodoComplete(todo: Todo) {
        dao.update(todo.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTodo(todo: Todo) = dao.update(todo.copy(deletedAt = System.currentTimeMillis()))

    suspend fun hardDeleteTodo(todo: Todo) = dao.delete(todo)

    fun getDeletedTodos(): Flow<List<Todo>> = dao.getDeletedTodos()

    suspend fun purgeOldDeletedItems(olderThan: Long) {
        dao.purgeDeletedTodos(olderThan)
    }
}
