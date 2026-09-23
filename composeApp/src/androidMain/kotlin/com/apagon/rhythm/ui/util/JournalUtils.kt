package com.apagon.rhythm.ui.util

import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.core.json.JSONArray

fun parseJsonStringList(json: String): List<String> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }
}

fun List<String>.toJsonString(): String = JSONArray(this).toString()

fun JournalEntry.tagList(): List<String> = parseJsonStringList(tags)
fun JournalEntry.photoUriList(): List<String> = parseJsonStringList(photoUris)
fun JournalEntry.feelingList(): List<String> = parseJsonStringList(feelings)
