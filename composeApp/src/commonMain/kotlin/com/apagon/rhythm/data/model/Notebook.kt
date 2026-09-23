package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class Notebook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorIndex: Int = 0,
    val colorArgb: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
