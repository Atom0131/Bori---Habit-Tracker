package com.apagon.rhythm.data.sync

import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.model.HabitFrequency
import kotlinx.serialization.Serializable

/**
 * Wire shapes for the sync engine (Stage 4). Deliberately flat, plain data —
 * no Ktor/transport types here, so this file (and SyncEngine.kt) stay usable
 * from commonMain even though the actual transport (SyncServer/SyncClient)
 * lives in jvmMain only.
 */
@Serializable
data class HabitDto(
    val syncId: String,
    val name: String,
    val description: String,
    val frequency: String,
    val targetDaysPerWeek: Int,
    val targetDaysPerMonth: Int,
    val weekDaysMask: Int,
    val monthDaysMask: Int,
    val reminderTime: String?,
    val createdAt: Long,
    val isActive: Boolean,
    val isChecklist: Boolean,
    val colorIndex: Int,
    val colorArgb: Int?,
    val durationDays: Int,
    val iconIndex: Int,
    val deletedAt: Long?,
    val soundUri: String,
    val vibrationPatternId: String,
    val updatedAt: Long
)

fun Habit.toDto() = HabitDto(
    syncId = syncId,
    name = name,
    description = description,
    frequency = frequency.name,
    targetDaysPerWeek = targetDaysPerWeek,
    targetDaysPerMonth = targetDaysPerMonth,
    weekDaysMask = weekDaysMask,
    monthDaysMask = monthDaysMask,
    reminderTime = reminderTime,
    createdAt = createdAt,
    isActive = isActive,
    isChecklist = isChecklist,
    colorIndex = colorIndex,
    colorArgb = colorArgb,
    durationDays = durationDays,
    iconIndex = iconIndex,
    deletedAt = deletedAt,
    soundUri = soundUri,
    vibrationPatternId = vibrationPatternId,
    updatedAt = updatedAt
)

/** id is left at Room's default (0 = autogenerate) — sync identity is syncId, never the local row id, which is only ever meaningful within one device's own database. */
fun HabitDto.toEntity() = Habit(
    name = name,
    description = description,
    frequency = HabitFrequency.valueOf(frequency),
    targetDaysPerWeek = targetDaysPerWeek,
    targetDaysPerMonth = targetDaysPerMonth,
    weekDaysMask = weekDaysMask,
    monthDaysMask = monthDaysMask,
    reminderTime = reminderTime,
    createdAt = createdAt,
    isActive = isActive,
    isChecklist = isChecklist,
    colorIndex = colorIndex,
    colorArgb = colorArgb,
    durationDays = durationDays,
    iconIndex = iconIndex,
    deletedAt = deletedAt,
    soundUri = soundUri,
    vibrationPatternId = vibrationPatternId,
    syncId = syncId,
    updatedAt = updatedAt
)

@Serializable
data class HabitCompletionDto(
    val syncId: String,
    /** The owning habit's syncId, NOT its local Room id — ids are per-database and collide across two independent SQLite files. */
    val habitSyncId: String,
    val dateCompleted: String,
    val updatedAt: Long,
    val deletedAt: Long?
)

fun HabitCompletion.toDto(habitSyncId: String) = HabitCompletionDto(
    syncId = syncId,
    habitSyncId = habitSyncId,
    dateCompleted = dateCompleted,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

@Serializable
data class SyncBatch(
    val deviceId: String,
    val habits: List<HabitDto>,
    val completions: List<HabitCompletionDto>
)
