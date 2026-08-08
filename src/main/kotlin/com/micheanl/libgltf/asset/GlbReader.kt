package com.micheanl.libgltf.asset

/**
 * libgltf · GlbReader
 *
 * <pre>{@code
 * val payload = if (GlbReader.isGlb(source)) {
 * }</pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import java.nio.ByteBuffer
import java.nio.ByteOrder

object GlbReader {
    private const val MAGIC: Int = 0x46546C67
    private const val VERSION: Int = 2
    private const val JSON: Int = 0x4E4F534A
    private const val BIN: Int = 0x004E4942

    fun read(source: ByteBuffer): GlbPayload {
        val buffer = source.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        require(buffer.remaining() >= 12)
        require(buffer.int == MAGIC)
        require(buffer.int == VERSION)
        val length = buffer.int
        require(length <= buffer.limit())
        var json = ByteArray(0)
        var binary: ByteBuffer? = null
        while (buffer.position() + 8 <= length) {
            val chunkLength = buffer.int
            val chunkType = buffer.int
            require(chunkLength >= 0 && buffer.position() + chunkLength <= length)
            val chunk = buffer.slice(buffer.position(), chunkLength).order(ByteOrder.LITTLE_ENDIAN)
            when (chunkType) {
                JSON -> {
                    json = ByteArray(chunkLength)
                    chunk.get(json)
                }
                BIN -> binary = chunk.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
            }
            buffer.position(buffer.position() + chunkLength)
        }
        require(json.isNotEmpty())
        return GlbPayload(json, binary)
    }

    fun isGlb(source: ByteBuffer): Boolean = source.remaining() >= 4 && source.duplicate().order(ByteOrder.LITTLE_ENDIAN).int == MAGIC
}
