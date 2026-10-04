package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(tableName = "timers", indices = [Index(value = ["syncId"], unique = true)])
data class Timer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val durationSeconds: Int,
    val remainingSeconds: Int,
    val endTimeMillis: Long = 0,  // 0 = paused/stopped; >0 = running (epoch ms when timer ends)
    val soundUri: String = "",    // "" = default alarm sound, "silent" = no sound, else URI string
    val vibrationPatternId: String = "default",
    val createdAt: Long = System.currentTimeMillis(),
    // Pomodoro fields (only used when isPomo = true)
    val isPomo: Boolean = false,
    val pomoWorkSecs: Int = 1500,           // 25 min default
    val pomoShortBreakSecs: Int = 300,      // 5 min default
    val pomoLongBreakSecs: Int = 900,       // 15 min default
    val pomoSessionsPerRound: Int = 4,
    val pomoCurrentSession: Int = 1,        // 1-indexed current work session
    val pomoPhase: String = "WORK",         // "WORK", "SHORT_BREAK", "LONG_BREAK"
    val deletedAt: Long? = null,
    /** Stable cross-device id for the Tailscale sync engine. Minted once, never overwritten. */
    val syncId: String = Uuid.random().toString(),
    /** Bumped on every local mutation; sync's last-write-wins conflict signal. */
    val updatedAt: Long = System.currentTimeMillis()
)
