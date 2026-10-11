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
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(
        entity = Habit::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["habitId"]), Index(value = ["syncId"], unique = true)]
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val label: String,
    val sortOrder: Int = 0,
    /** Stable cross-device id for the Tailscale sync engine. Minted once, never overwritten. */
    val syncId: String = Uuid.random().toString(),
    /** Bumped on every local mutation; sync's last-write-wins conflict signal. */
    val updatedAt: Long = System.currentTimeMillis(),
    /**
     * A routine step's time in seconds, 0 = untimed. Set on the phone, which runs routines; carried
     * here so a desktop edit or a sync round trip never drops it.
     */
    val durationSeconds: Int = 0
)
