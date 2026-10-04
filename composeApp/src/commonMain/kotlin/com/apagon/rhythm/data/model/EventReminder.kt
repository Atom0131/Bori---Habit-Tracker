package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * One alert attached to a [CalendarEvent]. An event may carry several.
 *
 * Exactly one of [minutesBefore] and [absoluteDateTime] is set: the first anchors the alert to the
 * event's start so it follows the event when it is moved, the second pins it to a wall-clock time
 * the user chose by hand.
 *
 * Ported from Android's `data/model/EventReminder.kt` for backup/sync schema parity — desktop has
 * no event-reminder scheduling UI yet (DAO + entity only, this pass).
 */
@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "event_reminders",
    foreignKeys = [
        ForeignKey(
            entity = CalendarEvent::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index(value = ["syncId"], unique = true)]
)
data class EventReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    /** Minutes before the event's start. Whole days (multiples of 1440) for an all-day event. */
    val minutesBefore: Int? = null,
    /** "yyyy-MM-dd HH:mm" — a fixed time, independent of the event. */
    val absoluteDateTime: String? = null,
    val soundUri: String = "",
    val vibrationPatternId: String = "default",
    val createdAt: Long = System.currentTimeMillis(),
    /** Stable cross-device id for the Tailscale sync engine. Minted once, never overwritten. */
    val syncId: String = Uuid.random().toString(),
    /** Bumped on every local mutation; sync's last-write-wins conflict signal. */
    val updatedAt: Long = System.currentTimeMillis()
)
