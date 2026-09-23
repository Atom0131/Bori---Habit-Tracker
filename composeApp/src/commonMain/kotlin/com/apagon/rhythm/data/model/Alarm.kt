package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * repeatDaysMask bitmask: bit0=Mon, bit1=Tue, bit2=Wed, bit3=Thu, bit4=Fri, bit5=Sat, bit6=Sun.
 * repeatDaysMask == 0 means one-time: auto-disabled after firing.
 */
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val hour: Int,
    val minute: Int,
    @ColumnInfo(name = "repeatDays") val repeatDaysMask: Int = 0,
    val isEnabled: Boolean = true,
    val soundUri: String = "",
    val vibrationPatternId: String = "default",
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
