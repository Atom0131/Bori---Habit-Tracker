package com.apagon.rhythm.data.model

import com.apagon.rhythm.core.time.System

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(tableName = "notebooks", indices = [Index(value = ["syncId"], unique = true)])
data class Notebook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorIndex: Int = 0,
    val colorArgb: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    // Ported from Android for backup/sync schema parity. Inert pass-through on desktop — no
    // private-notebook lock UI, no nested-notebook UI, no Markdown vault feature here.
    val isPrivate: Boolean = false,
    val vaultFolderName: String? = null,
    val folderDocUriCache: String? = null,
    val parentId: Long? = null,
    /** Stable cross-device id for the Tailscale sync engine. Minted once, never overwritten. */
    val syncId: String = Uuid.random().toString(),
    /**
     * Null until this notebook first crosses the wire with a peer (sent out or merged in).
     * Unlike Android, desktop mints [syncId] eagerly at row creation rather than lazily on first
     * sync, so syncId is never null here and can't double as a "never synced" signal. This field
     * exists only to give SyncEngine's unsynced-notebook-by-name dedup the same gate Android's
     * `syncId IS NULL` check gives it there — see SyncEngine.kt. Never sent over the wire.
     */
    val firstSyncedAt: Long? = null
)
