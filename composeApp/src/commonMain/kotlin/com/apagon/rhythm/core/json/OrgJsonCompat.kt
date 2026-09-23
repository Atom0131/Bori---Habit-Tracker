package com.apagon.rhythm.core.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * Drop-in replacement for org.json's JSONObject/JSONArray backed by
 * kotlinx-serialization, so backup code runs on iOS unchanged and the emitted
 * JSON stays structurally identical to backups from the Play Store app.
 */

private val prettyJson = Json { prettyPrint = true; prettyPrintIndent = "  " }

class JSONException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

class JSONObject {

    private val map = LinkedHashMap<String, JsonElement>()

    constructor()

    constructor(json: String) {
        val parsed = try {
            Json.parseToJsonElement(json)
        } catch (e: Exception) {
            throw JSONException("Invalid JSON", e)
        }
        map.putAll((parsed as? JsonObject) ?: throw JSONException("Not a JSON object"))
    }

    internal constructor(obj: JsonObject) {
        map.putAll(obj)
    }

    companion object {
        /** Sentinel for explicit nulls, like org.json.JSONObject.NULL. */
        val NULL: Any = JsonNull
    }

    fun put(key: String, value: Any?): JSONObject {
        map[key] = toElement(value)
        return this
    }

    fun has(key: String): Boolean = map.containsKey(key)

    /** True if the key is absent OR explicitly null — matching org.json. */
    fun isNull(key: String): Boolean = map[key].let { it == null || it is JsonNull }

    fun getString(key: String): String =
        (map[key] as? JsonPrimitive)?.contentOrNull ?: throw JSONException("No string value for $key")

    fun optString(key: String, fallback: String = ""): String =
        (map[key] as? JsonPrimitive)?.contentOrNull ?: fallback

    fun getInt(key: String): Int =
        (map[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() }
            ?: throw JSONException("No int value for $key")

    fun optInt(key: String, fallback: Int = 0): Int =
        (map[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() } ?: fallback

    fun getLong(key: String): Long =
        (map[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }
            ?: throw JSONException("No long value for $key")

    fun optLong(key: String, fallback: Long = 0L): Long =
        (map[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: fallback

    fun getBoolean(key: String): Boolean =
        (map[key] as? JsonPrimitive)?.booleanOrNull ?: throw JSONException("No boolean value for $key")

    fun optBoolean(key: String, fallback: Boolean = false): Boolean =
        (map[key] as? JsonPrimitive)?.booleanOrNull ?: fallback

    fun getJSONArray(key: String): JSONArray =
        (map[key] as? JsonArray)?.let { JSONArray(it) } ?: throw JSONException("No array value for $key")

    fun optJSONArray(key: String): JSONArray? = (map[key] as? JsonArray)?.let { JSONArray(it) }

    fun getJSONObject(key: String): JSONObject =
        (map[key] as? JsonObject)?.let { JSONObject(it) } ?: throw JSONException("No object value for $key")

    fun optJSONObject(key: String): JSONObject? = (map[key] as? JsonObject)?.let { JSONObject(it) }

    internal fun toJsonObject(): JsonObject = JsonObject(map)

    override fun toString(): String = toJsonObject().toString()

    fun toString(@Suppress("UNUSED_PARAMETER") indentSpaces: Int): String =
        prettyJson.encodeToString(JsonObject.serializer(), toJsonObject())
}

class JSONArray {

    private val items = ArrayList<JsonElement>()

    constructor()

    constructor(json: String) {
        val parsed = try {
            Json.parseToJsonElement(json)
        } catch (e: Exception) {
            throw JSONException("Invalid JSON", e)
        }
        items.addAll((parsed as? JsonArray) ?: throw JSONException("Not a JSON array"))
    }

    internal constructor(arr: JsonArray) {
        items.addAll(arr)
    }

    constructor(collection: Collection<*>) {
        collection.forEach { items.add(toElement(it)) }
    }

    fun put(value: Any?): JSONArray {
        items.add(toElement(value))
        return this
    }

    fun length(): Int = items.size

    fun getJSONObject(index: Int): JSONObject = JSONObject(items[index].jsonObject)

    fun getJSONArray(index: Int): JSONArray = JSONArray(items[index].jsonArray)

    fun getString(index: Int): String =
        (items[index] as? JsonPrimitive)?.contentOrNull ?: throw JSONException("No string at $index")

    internal fun toJsonArray(): JsonArray = JsonArray(items)

    override fun toString(): String = toJsonArray().toString()
}

private fun toElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is JSONObject -> value.toJsonObject()
    is JSONArray -> value.toJsonArray()
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Int -> JsonPrimitive(value)
    is Long -> JsonPrimitive(value)
    is Float -> JsonPrimitive(value)
    is Double -> JsonPrimitive(value)
    else -> JsonPrimitive(value.toString())
}
