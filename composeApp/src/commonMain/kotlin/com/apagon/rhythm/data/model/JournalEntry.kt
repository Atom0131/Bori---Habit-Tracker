package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("habitId"), Index("date")]
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,              // "yyyy-MM-dd"
    val title: String = "",
    val content: String = "",
    val mood: Int = 0,             // 0 = unset, 1–5
    val habitId: Long? = null,     // null = general daily entry; non-null = per-habit note
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tags: String = "",       // JSON array: ["workout","grateful"]
    val photoUris: String = "",  // JSON array of content:// URIs
    val feelings: String = "",   // JSON array: ["happy","calm"]
    val deletedAt: Long? = null
)
