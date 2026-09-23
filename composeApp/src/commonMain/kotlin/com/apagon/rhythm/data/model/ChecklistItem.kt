package com.apagon.rhythm.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(
        entity = Habit::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["habitId"])]
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val label: String,
    val sortOrder: Int = 0
)
