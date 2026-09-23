package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.Index

enum class TodoPriority { HIGH, MEDIUM, LOW, NONE }


@Entity(
    tableName = "todos",
    indices = [
        Index("isCompleted"),
        Index("completedAt")
    ]
)
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val dueDate: String = "",           // "yyyy-MM-dd HH:mm" or "yyyy-MM-dd" or "" if not set
    val priority: String = TodoPriority.NONE.name,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val iconIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val deletedAt: Long? = null,
    val soundUri: String = "",
    val vibrationPatternId: String = "default"
)
