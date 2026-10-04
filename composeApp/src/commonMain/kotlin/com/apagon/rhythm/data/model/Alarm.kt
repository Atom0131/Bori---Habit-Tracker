package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * repeatDaysMask bitmask: bit0=Sun, bit1=Mon, bit2=Tue, bit3=Wed, bit4=Thu, bit5=Fri, bit6=Sat.
 * repeatDaysMask == 0 means one-time: auto-disabled after firing.
 *
 * Note the Kotlin property is `repeatDaysMask` but the column is `repeatDays` — any raw SQL
 * (migrations especially) must use the column name. commonMain's (dead-code) `MIGRATION_32_33`
 * got this wrong and crashed; see this file's own `DESKTOP_HABIT_MIGRATION_*` for the convention.
 *
 * Standardized on Android's bit order (previously bit0=Mon on desktop) — see
 * `DesktopHabitDatabase.kt`'s data migration for the one-time remap of existing rows.
 */
@OptIn(ExperimentalUuidApi::class)
@Entity(tableName = "alarms", indices = [Index(value = ["syncId"], unique = true)])
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val hour: Int,
    val minute: Int,
    @ColumnInfo(name = "repeatDays") val repeatDaysMask: Int = 0,
    val isEnabled: Boolean = true,
    val soundUri: String = "",
    val vibrationPatternId: String = "default",
    // Ported from Android for backup/sync schema parity. Inert pass-through on desktop — no
    // wake-up-check UI here.
    val dismissMission: String = "none",
    val missionDifficulty: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    /** Stable cross-device id for the Tailscale sync engine. Minted once, never overwritten. */
    val syncId: String = Uuid.random().toString(),
    /** Bumped on every local mutation; sync's last-write-wins conflict signal. */
    val updatedAt: Long = System.currentTimeMillis()
)
