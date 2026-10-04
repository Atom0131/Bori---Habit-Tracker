package com.apagon.rhythm.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.apagon.rhythm.data.model.Todo
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos WHERE isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL ORDER BY createdAt ASC")
    fun getPendingTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE isArchived = 1 AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getArchivedTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE isCompleted = 1 AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getCompletedTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE isCompleted = 1 AND deletedAt IS NULL AND completedAt >= :dayStart AND completedAt < :dayEnd ORDER BY completedAt DESC")
    fun getCompletedTodosForDay(dayStart: Long, dayEnd: Long): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE deletedAt IS NULL AND isArchived = 0")
    fun getAllActiveTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedTodos(): Flow<List<Todo>>

    @Query("DELETE FROM todos WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun purgeDeletedTodos(olderThan: Long)

    @Insert
    suspend fun insert(todo: Todo): Long

    @Update
    suspend fun update(todo: Todo)

    @Query("UPDATE todos SET isCompleted = 1, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markCompleteById(id: Long, completedAt: Long, updatedAt: Long)

    @Delete
    suspend fun delete(todo: Todo)

    // ── Sync (Stage 2) ───────────────────────────────────────────────────────

    @Query("SELECT * FROM todos WHERE syncId = :syncId")
    suspend fun getTodoBySyncId(syncId: String): Todo?

    @Query("SELECT * FROM todos WHERE updatedAt > :since")
    suspend fun getTodosUpdatedSince(since: Long): List<Todo>

    @Query("SELECT * FROM todos")
    suspend fun getAllTodosForBackup(): List<Todo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<Todo>)

    @Query("DELETE FROM todos")
    suspend fun deleteAll()
}
