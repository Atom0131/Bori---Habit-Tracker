package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val dateTime: String,  // "yyyy-MM-dd HH:mm"
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val soundUri: String = "",
    val deletedAt: Long? = null
)
