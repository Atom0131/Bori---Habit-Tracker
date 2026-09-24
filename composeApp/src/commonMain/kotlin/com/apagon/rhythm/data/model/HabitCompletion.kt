package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
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
        Index(value = ["dateCompleted"]),
        Index(value = ["syncId"], unique = true)
    ]
)
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    /** ISO date string in "yyyy-MM-dd" format. One row per habit per day. */
    val dateCompleted: String,
    /** Stable cross-device id for the Tailscale sync engine. Minted once, never overwritten. */
    val syncId: String = Uuid.random().toString(),
    /** Bumped on every local mutation; sync's last-write-wins conflict signal. */
    val updatedAt: Long = System.currentTimeMillis(),
    /**
     * Soft-delete tombstone so an "uncheck" propagates to a sync peer instead
     * of silently vanishing. Non-null = the day was unmarked; still occupies
     * the unique (habitId, dateCompleted) slot until purged, so re-completing
     * that day must revive this row rather than insert a new one (see
     * HabitDao.insertOrReviveCompletion).
     */
    val deletedAt: Long? = null
)
