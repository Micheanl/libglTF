package com.micheanl.libgltf.util

/**
 * libgltf · JsonValue
 *
 * <pre>{@code
 * val root = JsonValue(
 * json.decodeFromStream(
 * JsonElement.serializer(),
 * ByteArrayInputStream(payload.json)
 * )
 * )
 * }</pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

@JvmInline
internal value class JsonValue(val element: JsonElement) {
    fun size(): Int = (element as JsonArray).size

    operator fun get(index: Int): JsonValue = JsonValue((element as JsonArray)[index])
}
