package com.apagon.rhythm.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["habitId", "dateCompleted"], unique = true),
        Index(value = ["dateCompleted"])
    ]
)
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    /** ISO date string in "yyyy-MM-dd" format. One row per habit per day. */
    val dateCompleted: String
)
