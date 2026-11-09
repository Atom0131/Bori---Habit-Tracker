package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(tableName = "calendar_events", indices = [Index(value = ["syncId"], unique = true)])
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
    val deletedAt: Long? = null,
    /** Stable cross-device id for the Tailscale sync engine. Minted once, never overwritten. */
    val syncId: String = Uuid.random().toString(),
    /** Bumped on every local mutation; sync's last-write-wins conflict signal. */
    val updatedAt: Long = System.currentTimeMillis(),
    /** How it repeats, as a [Recurrence] rule ("" = doesn't repeat). Same string on Android. */
    @ColumnInfo(defaultValue = "") val recurrence: String = "",
    /** Dates of the series that were skipped or split off ("Only this event"), comma-separated yyyy-MM-dd. */
    @ColumnInfo(defaultValue = "") val recurrenceExceptions: String = "",
    /**
     * Set only on a display copy of one date of a repeating series (see [expandForRange]): that date.
     * The copy keeps the series' id and syncId, so tapping it opens the series.
     */
    @Ignore val occurrenceDate: String? = null
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
        deletedAt: Long?,
        syncId: String,
        updatedAt: Long,
        recurrence: String,
        recurrenceExceptions: String
    ) : this(
        id, title, note, startDate, endDate, startTime, endTime,
        colorIndex, colorArgb, null, null, isActive, createdAt, deletedAt, syncId, updatedAt,
        recurrence, recurrenceExceptions, null
    )
}
