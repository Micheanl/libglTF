package com.micheanl.libgltf.asset

/**
 * libgltf · AccessorDecoder
 *
 * <pre>{@code
 * val decoder = AccessorDecoder(root, resolver)
 * }</pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.util.JsonValue
import com.micheanl.libgltf.util.JsonFields
import java.nio.ByteBuffer

internal class AccessorDecoder(
    private val root: JsonValue,
    private val resolver: GltfBufferResolver
) {
    fun readFloats(index: Int): FloatArray {
        val accessor = accessor(index)
        val componentCount = componentCount(JsonFields.string(accessor, "type"))
        val count = JsonFields.int(accessor, "count", 0)
        val result = FloatArray(count * componentCount)
        val viewIndex = JsonFields.int(accessor, "bufferView")
        val componentType = JsonFields.int(accessor, "componentType")
        val normalized = JsonFields.boolean(accessor, "normalized")
        if (viewIndex >= 0) {
            val source = resolver.view(viewIndex)
            val elementSize = elementSize(JsonFields.string(accessor, "type"), componentType)
            val stride = resolver.viewStride(viewIndex).takeIf { it > 0 } ?: elementSize
            val base = JsonFields.int(accessor, "byteOffset", 0)
            fillFloats(result, source, base, stride, count, componentCount, componentType, normalized)
        }
        applySparseFloats(accessor, result, componentCount, componentType, normalized)
        return result
    }

    fun readInts(index: Int): IntArray {
        val accessor = accessor(index)
        val componentCount = componentCount(JsonFields.string(accessor, "type"))
        val count = JsonFields.int(accessor, "count", 0)
        val result = IntArray(count * componentCount)
        val viewIndex = JsonFields.int(accessor, "bufferView")
        val componentType = JsonFields.int(accessor, "componentType")
        if (viewIndex >= 0) {
            val source = resolver.view(viewIndex)
            val elementSize = elementSize(JsonFields.string(accessor, "type"), componentType)
            val stride = resolver.viewStride(viewIndex).takeIf { it > 0 } ?: elementSize
            val base = JsonFields.int(accessor, "byteOffset", 0)
            fillInts(result, source, base, stride, count, componentCount, componentType)
        }
        applySparseInts(accessor, result, componentCount, componentType)
        return result
    }

    fun count(index: Int): Int = JsonFields.int(accessor(index), "count", 0)

    fun min(index: Int): FloatArray = JsonFields.floats(accessor(index), "min")

    fun max(index: Int): FloatArray = JsonFields.floats(accessor(index), "max")

    private fun accessor(index: Int): JsonValue {
        val accessors = JsonFields.value(root, "accessors") ?: error("accessors is missing")
        return accessors[index]
    }

    private fun applySparseFloats(
        accessor: JsonValue,
        target: FloatArray,
        componentCount: Int,
        componentType: Int,
        normalized: Boolean
    ) {
        val sparse = JsonFields.value(accessor, "sparse") ?: return
        val count = JsonFields.int(sparse, "count", 0)
        if (count == 0) return
        val indices = JsonFields.value(sparse, "indices") ?: error("sparse indices missing")
        val values = JsonFields.value(sparse, "values") ?: error("sparse values missing")
        val indexBuffer = resolver.view(JsonFields.int(indices, "bufferView"))
        val indexOffset = JsonFields.int(indices, "byteOffset", 0)
        val indexType = JsonFields.int(indices, "componentType")
        val valueBuffer = resolver.view(JsonFields.int(values, "bufferView"))
        val valueOffset = JsonFields.int(values, "byteOffset", 0)
        val componentSize = componentSize(componentType)
        for (entry in 0 until count) {
            val targetIndex = unsigned(indexBuffer, indexOffset + entry * componentSize(indexType), indexType)
            val output = targetIndex * componentCount
            val input = valueOffset + entry * componentCount * componentSize
            for (component in 0 until componentCount) {
                target[output + component] = float(valueBuffer, input + component * componentSize, componentType, normalized)
            }
        }
    }

    private fun applySparseInts(accessor: JsonValue, target: IntArray, componentCount: Int, componentType: Int) {
        val sparse = JsonFields.value(accessor, "sparse") ?: return
        val count = JsonFields.int(sparse, "count", 0)
        if (count == 0) return
        val indices = JsonFields.value(sparse, "indices") ?: error("sparse indices missing")
        val values = JsonFields.value(sparse, "values") ?: error("sparse values missing")
        val indexBuffer = resolver.view(JsonFields.int(indices, "bufferView"))
        val indexOffset = JsonFields.int(indices, "byteOffset", 0)
        val indexType = JsonFields.int(indices, "componentType")
        val valueBuffer = resolver.view(JsonFields.int(values, "bufferView"))
        val valueOffset = JsonFields.int(values, "byteOffset", 0)
        val componentSize = componentSize(componentType)
        for (entry in 0 until count) {
            val targetIndex = unsigned(indexBuffer, indexOffset + entry * componentSize(indexType), indexType)
            val output = targetIndex * componentCount
            val input = valueOffset + entry * componentCount * componentSize
            for (component in 0 until componentCount) {
                target[output + component] = integer(valueBuffer, input + component * componentSize, componentType)
            }
        }
    }

    private fun fillFloats(
        target: FloatArray,
        source: ByteBuffer,
        base: Int,
        stride: Int,
        count: Int,
        componentCount: Int,
        componentType: Int,
        normalized: Boolean
    ) {
        val size = componentSize(componentType)
        for (element in 0 until count) {
            val sourceBase = base + element * stride
            val targetBase = element * componentCount
            for (component in 0 until componentCount) {
                target[targetBase + component] = float(source, sourceBase + component * size, componentType, normalized)
            }
        }
    }

    private fun fillInts(
        target: IntArray,
        source: ByteBuffer,
        base: Int,
        stride: Int,
        count: Int,
        componentCount: Int,
        componentType: Int
    ) {
        val size = componentSize(componentType)
        for (element in 0 until count) {
            val sourceBase = base + element * stride
            val targetBase = element * componentCount
            for (component in 0 until componentCount) {
                target[targetBase + component] = integer(source, sourceBase + component * size, componentType)
            }
        }
    }

    private fun float(source: ByteBuffer, offset: Int, type: Int, normalized: Boolean): Float = when (type) {
        BYTE -> if (normalized) (source.get(offset).toInt() / 127.0f).coerceAtLeast(-1.0f) else source.get(offset).toFloat()
        UNSIGNED_BYTE -> if (normalized) (source.get(offset).toInt() and 0xFF) / 255.0f else (source.get(offset).toInt() and 0xFF).toFloat()
        SHORT -> if (normalized) (source.getShort(offset).toInt() / 32767.0f).coerceAtLeast(-1.0f) else source.getShort(offset).toFloat()
        UNSIGNED_SHORT -> if (normalized) (source.getShort(offset).toInt() and 0xFFFF) / 65535.0f else (source.getShort(offset).toInt() and 0xFFFF).toFloat()
        UNSIGNED_INT -> if (normalized) (source.getInt(offset).toLong() and 0xFFFFFFFFL) / 4294967295.0f else (source.getInt(offset).toLong() and 0xFFFFFFFFL).toFloat()
        FLOAT -> source.getFloat(offset)
        else -> error("unsupported component type $type")
    }

    private fun integer(source: ByteBuffer, offset: Int, type: Int): Int = when (type) {
        BYTE -> source.get(offset).toInt()
        UNSIGNED_BYTE -> source.get(offset).toInt() and 0xFF
        SHORT -> source.getShort(offset).toInt()
        UNSIGNED_SHORT -> source.getShort(offset).toInt() and 0xFFFF
        UNSIGNED_INT -> source.getInt(offset)
        else -> error("unsupported integer component type $type")
    }

    private fun unsigned(source: ByteBuffer, offset: Int, type: Int): Int = when (type) {
        UNSIGNED_BYTE -> source.get(offset).toInt() and 0xFF
        UNSIGNED_SHORT -> source.getShort(offset).toInt() and 0xFFFF
        UNSIGNED_INT -> source.getInt(offset)
        else -> error("unsupported sparse index type $type")
    }

    private fun componentCount(type: String): Int = when (type) {
        "SCALAR" -> 1
        "VEC2" -> 2
        "VEC3" -> 3
        "VEC4", "MAT2" -> 4
        "MAT3" -> 9
        "MAT4" -> 16
        else -> error("unsupported accessor type $type")
    }

    private fun componentSize(type: Int): Int = when (type) {
        BYTE, UNSIGNED_BYTE -> 1
        SHORT, UNSIGNED_SHORT -> 2
        UNSIGNED_INT, FLOAT -> 4
        else -> error("unsupported component type $type")
    }

    private fun elementSize(type: String, componentType: Int): Int = componentCount(type) * componentSize(componentType)

    private companion object {
        const val BYTE: Int = 5120
        const val UNSIGNED_BYTE: Int = 5121
        const val SHORT: Int = 5122
        const val UNSIGNED_SHORT: Int = 5123
        const val UNSIGNED_INT: Int = 5125
        const val FLOAT: Int = 5126
    }
}
