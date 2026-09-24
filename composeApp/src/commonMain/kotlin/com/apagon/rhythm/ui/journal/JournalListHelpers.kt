package com.apagon.rhythm.ui.journal

import com.apagon.rhythm.core.json.JSONArray
import com.apagon.rhythm.data.model.JournalEntry

// Local copy of androidMain's ui/util/JournalUtils.kt parsing helpers — that
// file lives in androidMain purely by placement (its own logic is already
// platform-agnostic JSONArray parsing), but duplicating three small
// functions here avoids reaching across source sets for it, matching this
// project's established "small self-contained files" preference.
internal fun parseJsonStringList(json: String): List<String> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun JournalEntry.tagList(): List<String> = parseJsonStringList(tags)
internal fun JournalEntry.photoUriList(): List<String> = parseJsonStringList(photoUris)
internal fun JournalEntry.feelingList(): List<String> = parseJsonStringList(feelings)

internal val journalFeelings = listOf(
    "Happy" to "😄",
    "Grateful" to "🙏",
    "Anxious" to "😰",
    "Excited" to "🎉",
    "Calm" to "😌",
    "Tired" to "😴",
    "Proud" to "💪",
    "Focused" to "🎯"
)
