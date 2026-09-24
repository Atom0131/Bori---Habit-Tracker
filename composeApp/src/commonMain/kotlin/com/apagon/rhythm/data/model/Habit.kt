package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class HabitFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}

@OptIn(ExperimentalUuidApi::class)
@Entity(tableName = "habits", indices = [Index(value = ["syncId"], unique = true)])
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    /** How many days per week the habit should be completed. Used for WEEKLY habits only. */
    val targetDaysPerWeek: Int = 1,
    /** How many days per month the habit should be completed. Used for MONTHLY habits only. */
    val targetDaysPerMonth: Int = 1,
    /** Bitmask of selected weekdays for WEEKLY habits. Bit 0 = Monday … bit 6 = Sunday. */
    val weekDaysMask: Int = 0,
    /** Bitmask of selected days-of-month for MONTHLY habits. Bit 0 = 1st … bit 30 = 31st. */
    val monthDaysMask: Int = 0,
    /** Optional reminder time in "HH:mm" format. */
    val reminderTime: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isChecklist: Boolean = false,
    val colorIndex: Int = 0,
    val colorArgb: Int? = null,
    /** Total days the user wants to maintain this habit. 0 = no goal set. */
    val durationDays: Int = 0,
    /** Index into habitIconLibrary. -1 = use colorIndex-based default icon. */
    val iconIndex: Int = -1,
    val deletedAt: Long? = null,
    val soundUri: String = "",
    val vibrationPatternId: String = "default",
    /** Stable cross-device id for the Tailscale sync engine. Minted once, never overwritten. */
    val syncId: String = Uuid.random().toString(),
    /** Bumped on every local mutation; sync's last-write-wins conflict signal. */
    val updatedAt: Long = System.currentTimeMillis()
)
