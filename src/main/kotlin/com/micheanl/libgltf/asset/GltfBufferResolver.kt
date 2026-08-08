package com.micheanl.libgltf.asset

/**
 * libgltf · GltfBufferResolver
 *
 * <pre><code>
 * val resolver = GltfBufferResolver(root, basePath, payload.binary)
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.util.JsonValue
import com.micheanl.libgltf.util.JsonFields
import org.lwjgl.util.meshoptimizer.MeshOptimizer
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

internal class GltfBufferResolver(
    private val root: JsonValue,
    private val basePath: Path,
    binaryChunk: ByteBuffer?
) {
    private val buffers: Array<ByteBuffer>
    private val decodedViews = HashMap<Int, ByteBuffer>()

    init {
        val values = JsonFields.value(root, "buffers")
        buffers = if (values == null) {
            if (binaryChunk == null) emptyArray() else arrayOf(binaryChunk.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN))
        } else {
            Array(values.size()) { index ->
                val value = values[index]
                val uri = JsonFields.string(value, "uri")
                val data = if (uri.isEmpty()) {
                    require(index == 0 && binaryChunk != null)
                    binaryChunk.asReadOnlyBuffer()
                } else {
                    ByteBuffer.wrap(readUri(uri))
                }
                data.order(ByteOrder.LITTLE_ENDIAN)
            }
        }
    }

    fun view(index: Int): ByteBuffer {
        val views = JsonFields.value(root, "bufferViews") ?: error("bufferViews is missing")
        val view = views[index]
        val meshopt = JsonFields.value(JsonFields.value(view, "extensions"), "EXT_meshopt_compression")
        if (meshopt != null) return decodedView(index, view, meshopt)
        val buffer = buffers[JsonFields.int(view, "buffer", 0)].duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val offset = JsonFields.int(view, "byteOffset", 0)
        val length = JsonFields.int(view, "byteLength", 0)
        return buffer.slice(offset, length).order(ByteOrder.LITTLE_ENDIAN)
    }

    fun viewStride(index: Int): Int {
        val views = JsonFields.value(root, "bufferViews") ?: error("bufferViews is missing")
        val view = views[index]
        val meshopt = JsonFields.value(JsonFields.value(view, "extensions"), "EXT_meshopt_compression")
        return if (meshopt != null) {
            JsonFields.int(meshopt, "byteStride", 0)
        } else {
            JsonFields.int(view, "byteStride", 0)
        }
    }

    private fun decodedView(index: Int, view: JsonValue, meshopt: JsonValue): ByteBuffer {
        decodedViews[index]?.let { return it.duplicate().order(ByteOrder.LITTLE_ENDIAN) }
        val buffer = buffers[JsonFields.int(meshopt, "buffer", 0)].duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val source = buffer.slice(
            JsonFields.int(meshopt, "byteOffset", 0),
            JsonFields.int(meshopt, "byteLength", 0)
        )
        val count = JsonFields.int(meshopt, "count", 0)
        val byteStride = JsonFields.int(meshopt, "byteStride", 0)
        val decoded = try {
            decodeMeshopt(source, count, byteStride, meshoptMode(meshopt), meshoptFilter(meshopt))
        } catch (failure: Throwable) {
            fallbackView(view, failure)
        }
        decodedViews[index] = decoded
        return decoded.duplicate().order(ByteOrder.LITTLE_ENDIAN)
    }

    private fun decodeMeshopt(source: ByteBuffer, count: Int, byteStride: Int, mode: Int, filter: Int): ByteBuffer {
        val destination = ByteBuffer.allocateDirect(count * byteStride).order(ByteOrder.nativeOrder())
        val result = if (mode == 0) {
            MeshOptimizer.meshopt_decodeVertexBuffer(destination, count.toLong(), byteStride.toLong(), source)
        } else {
            MeshOptimizer.meshopt_decodeIndexBuffer(destination, count.toLong(), byteStride.toLong(), source)
        }
        require(result == 0) { "meshopt decode failed with code $result" }
        if (mode == 0) {
            when (filter) {
                1 -> MeshOptimizer.meshopt_decodeFilterOct(destination, count.toLong(), byteStride.toLong())
                2 -> MeshOptimizer.meshopt_decodeFilterQuat(destination, count.toLong(), byteStride.toLong())
                3 -> MeshOptimizer.meshopt_decodeFilterExp(destination, count.toLong(), byteStride.toLong())
            }
        }
        return destination
    }

    private fun fallbackView(view: JsonValue, failure: Throwable): ByteBuffer {
        val fallbackIndex = JsonFields.int(
            JsonFields.value(JsonFields.value(view, "extensions"), "EXT_meshopt_compression"),
            "fallback",
            -1
        )
        if (fallbackIndex >= 0) return view(fallbackIndex)
        throw failure
    }

    private fun meshoptMode(meshopt: JsonValue): Int {
        val named = JsonFields.string(meshopt, "mode")
        if (named.isNotEmpty()) {
            return when (named) {
                "ATTRIBUTES" -> 0
                "TRIANGLES" -> 1
                "INDICES" -> 2
                else -> error("unsupported meshopt mode $named")
            }
        }
        val numeric = JsonFields.int(meshopt, "mode", -1)
        if (numeric == 0 || numeric == 1 || numeric == 2) return numeric
        error("meshopt mode is missing")
    }

    private fun meshoptFilter(meshopt: JsonValue): Int {
        val named = JsonFields.string(meshopt, "filter")
        if (named.isNotEmpty()) {
            return when (named) {
                "NONE" -> 0
                "OCTAHEDRAL" -> 1
                "QUATERNION" -> 2
                "EXPONENTIAL" -> 3
                else -> error("unsupported meshopt filter $named")
            }
        }
        return JsonFields.int(meshopt, "filter", 0)
    }

    fun readUri(uri: String): ByteArray {
        if (uri.startsWith("data:")) {
            val comma = uri.indexOf(',')
            require(comma >= 0)
            val metadata = uri.substring(5, comma)
            val payload = uri.substring(comma + 1)
            return if (metadata.endsWith(";base64")) {
                Base64.getDecoder().decode(payload)
            } else {
                URLDecoder.decode(payload, StandardCharsets.UTF_8).toByteArray(StandardCharsets.UTF_8)
            }
        }
        val decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8)
        return Files.readAllBytes(basePath.resolve(decoded).normalize())
    }
}
