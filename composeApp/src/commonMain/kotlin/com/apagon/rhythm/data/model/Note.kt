package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Notebook::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["notebookId"])]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notebookId: Long,
    val title: String,
    val content: String, // Stored as JSON for structured/block content
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val deletedAt: Long? = null,
    val tags: String = "",
    val fontFamily: String = "default",
    val fontSize: String = "normal"
)
