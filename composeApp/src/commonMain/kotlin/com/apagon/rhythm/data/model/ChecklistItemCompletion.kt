package com.apagon.rhythm.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklist_item_completions",
    foreignKeys = [ForeignKey(
        entity = ChecklistItem::class,
        parentColumns = ["id"],
        childColumns = ["itemId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["itemId"]),
        Index(value = ["itemId", "dateCompleted"], unique = true)
    ]
)
data class ChecklistItemCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val dateCompleted: String  // "yyyy-MM-dd"
)
