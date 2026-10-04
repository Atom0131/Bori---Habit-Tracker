package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * One item of a to-do's checklist. Ported from Android for backup/sync schema parity — desktop's
 * to-do editor doesn't build nested checklists yet (DAO + entity only, this pass).
 *
 * [parentId] gives one extra level of nesting, matching Android's shape. **There is deliberately
 * no foreign key on [parentId], only an index** — same reasoning as Android's: a self-referencing
 * FK cannot be attached by `ALTER TABLE ADD COLUMN` without a full table rebuild, and a future
 * `insertAllSubtasks` REPLACE-batch restore (ordered by an unordered `getAllSubtasksForBackup()`
 * SELECT) could list a child before its parent and roll back the whole restore if the FK were
 * enforced.
 */
@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "todo_subtasks",
    foreignKeys = [ForeignKey(
        entity = Todo::class,
        parentColumns = ["id"],
        childColumns = ["todoId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["todoId"]),
        Index(value = ["parentId"]),
        Index(value = ["syncId"], unique = true)
    ]
)
data class TodoSubtask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val todoId: Long,
    val label: String,
    val isDone: Boolean = false,
    val sortOrder: Int = 0,
    val parentId: Long? = null,
    /** Stable cross-device id for the Tailscale sync engine. Minted once, never overwritten. */
    val syncId: String = Uuid.random().toString(),
    /** Bumped on every local mutation; sync's last-write-wins conflict signal. */
    val updatedAt: Long = System.currentTimeMillis()
)
