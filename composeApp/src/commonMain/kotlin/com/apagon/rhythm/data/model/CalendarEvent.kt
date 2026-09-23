package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val startDate: String,          // "yyyy-MM-dd"
    val endDate: String,            // "yyyy-MM-dd", equals startDate for single-day
    val startTime: String? = null,  // "HH:mm", null = all-day
    val endTime: String? = null,    // "HH:mm"
    val colorIndex: Int = 0,
    val colorArgb: Int? = null,
    @Ignore val calendarDisplayName: String? = null,
    @Ignore val accountName: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
) {
    // Room (KSP) requires a constructor whose parameters all map to DB columns.
    // The secondary constructor here omits the @Ignore fields so Room can use it.
    constructor(
        id: Long,
        title: String,
        note: String,
        startDate: String,
        endDate: String,
        startTime: String?,
        endTime: String?,
        colorIndex: Int,
        colorArgb: Int?,
        isActive: Boolean,
        createdAt: Long,
        deletedAt: Long?
    ) : this(
        id, title, note, startDate, endDate, startTime, endTime,
        colorIndex, colorArgb, null, null, isActive, createdAt, deletedAt
    )
}
