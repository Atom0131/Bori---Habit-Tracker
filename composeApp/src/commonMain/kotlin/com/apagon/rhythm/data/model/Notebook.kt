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
    val syncId: String = Uuid.random().toString()
)
