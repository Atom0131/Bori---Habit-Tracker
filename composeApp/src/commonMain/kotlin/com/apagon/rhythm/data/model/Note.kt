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
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Notebook::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["notebookId"]), Index(value = ["syncId"], unique = true)]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notebookId: Long,
    val title: String,
    val content: String, // Stored as JSON for structured/block content
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val deletedAt: Long? = null,
    val tags: String = "",
    val fontFamily: String = "default",
    val fontSize: String = "normal",
    // Ported from Android for backup/sync schema parity.
    val bodyPreview: String = "",        // plain-text cache for list previews
    val filePointer: String? = null,     // Markdown-vault feature desktop doesn't have — inert pass-through
    val fileDocUriCache: String? = null, // perf-only, self-healing — never authoritative, never backed up
    val fileSyncedAt: Long? = null,      // Markdown-vault feature desktop doesn't have — inert pass-through
    val uuid: String? = null,            // stable external-facing identifier; see Android's Note.kt KDoc
    /** Stable cross-device id for the Tailscale sync engine. Minted once, never overwritten. */
    val syncId: String = Uuid.random().toString()
)
