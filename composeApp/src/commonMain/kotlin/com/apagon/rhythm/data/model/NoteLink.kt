package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// One row per LINK block: sourceNoteId's content links to targetNoteId.
// Ported from Android for backup/sync schema parity — desktop's note editor doesn't generate
// links yet; this exists purely so links created on Android round-trip losslessly.
@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "note_links",
    indices = [
        Index("sourceNoteId"),
        Index("targetNoteId"),
        Index(value = ["syncId"], unique = true)
    ]
)
data class NoteLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceNoteId: Long,
    val targetNoteId: Long,
    /** Stable cross-device id for the Tailscale sync engine. Minted once, never overwritten. */
    val syncId: String = Uuid.random().toString(),
    /** Bumped on every local mutation; sync's last-write-wins conflict signal. */
    val updatedAt: Long = System.currentTimeMillis()
)
