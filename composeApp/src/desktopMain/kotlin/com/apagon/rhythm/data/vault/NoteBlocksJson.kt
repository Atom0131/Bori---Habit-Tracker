package com.apagon.rhythm.data.vault

import com.apagon.rhythm.core.json.JSONArray
import com.apagon.rhythm.core.json.JSONObject
import com.apagon.rhythm.ui.notes.BlockType
import com.apagon.rhythm.ui.notes.NoteBlock

/** `Note.content` is the editor's block list as JSON. Shared by the vault sync and the agent API. */
object NoteBlocksJson {

    fun encode(blocks: List<NoteBlock>): String {
        val array = JSONArray()
        blocks.forEach { b ->
            array.put(JSONObject().apply {
                put("id", b.id); put("type", b.type.name); put("content", b.content); put("isChecked", b.isChecked)
            })
        }
        return array.toString()
    }

    fun decode(json: String): List<NoteBlock> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                NoteBlock(
                    id = o.optString("id", ""),
                    type = runCatching { BlockType.valueOf(o.optString("type", "TEXT").uppercase()) }.getOrDefault(BlockType.TEXT),
                    content = o.optString("content", ""),
                    isChecked = o.optBoolean("isChecked", false)
                )
            }
        }.getOrElse { listOf(NoteBlock(content = json)) }
    }

    /** The list-row preview: the text of the note, no ink or pictures. */
    fun preview(blocks: List<NoteBlock>) = blocks
        .filter { it.type != BlockType.INK && it.type != BlockType.IMAGE && it.content.isNotBlank() }
        .joinToString(" ") { it.content.replace('\n', ' ') }.take(500)
}
