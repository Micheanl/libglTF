package com.micheanl.libgltf.util

/**
 * libgltf · JsonFields
 *
 * ```
 * val componentCount = componentCount(JsonFields.string(accessor, "type"))
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

internal object JsonFields {
    fun value(source: JsonValue?, key: String): JsonValue? {
        val element = (source?.element as? JsonObject)?.get(key) ?: return null
        return if (element is JsonNull) null else JsonValue(element)
    }

    fun string(source: JsonValue?, key: String, fallback: String = ""): String =
        value(source, key)?.element?.jsonPrimitive?.contentOrNull ?: fallback

    fun int(source: JsonValue?, key: String, fallback: Int = -1): Int =
        value(source, key)?.element?.jsonPrimitive?.intOrNull ?: fallback

    fun float(source: JsonValue?, key: String, fallback: Float = 0.0f): Float =
        value(source, key)?.element?.jsonPrimitive?.floatOrNull ?: fallback

    fun boolean(source: JsonValue?, key: String, fallback: Boolean = false): Boolean =
        value(source, key)?.element?.jsonPrimitive?.booleanOrNull ?: fallback

    fun ints(source: JsonValue?, key: String): IntArray {
        val array = value(source, key) ?: return IntArray(0)
        return IntArray(array.size()) { array[it].element.jsonPrimitive.intOrNull ?: 0 }
    }

    fun floats(source: JsonValue?, key: String, fallback: FloatArray = FloatArray(0)): FloatArray {
        val array = value(source, key) ?: return fallback
        return FloatArray(array.size()) { array[it].element.jsonPrimitive.floatOrNull ?: 0.0f }
    }
}
