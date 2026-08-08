package com.micheanl.libgltf.lod

import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.meshoptimizer.MeshOptimizer
import java.nio.FloatBuffer
import java.nio.IntBuffer


/**
 * libgltf · MeshLodBuilder
 *
 * <pre><code>
 * MeshLodBuilder.build(indices, positions, lodPolicy.triangleRatios)
 * </code></pre>
 *
 * LOD 网格简化
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object MeshLodBuilder {
    fun build(indices: IntArray, positions: FloatArray, ratios: FloatArray): Array<IntArray> {
        if (indices.size < 12 || ratios.size == 1) return arrayOf(indices)
        val source = MemoryUtil.memAllocInt(indices.size)
        val vertices = MemoryUtil.memAllocFloat(positions.size)
        return try {
            source.put(indices).flip()
            vertices.put(positions).flip()
            Array(ratios.size) { level ->
                if (level == 0 || ratios[level] >= 0.999f) {
                    indices
                } else {
                    simplify(source, vertices, positions.size / 3, indices.size, ratios[level])
                }
            }
        } finally {
            MemoryUtil.memFree(vertices)
            MemoryUtil.memFree(source)
        }
    }

    private fun simplify(
        source: IntBuffer,
        positions: FloatBuffer,
        vertexCount: Int,
        sourceCount: Int,
        ratio: Float
    ): IntArray {
        val targetCount = ((sourceCount * ratio).toInt() / 3 * 3).coerceIn(3, sourceCount)
        val destination = MemoryUtil.memAllocInt(sourceCount)
        return try {
            source.position(0).limit(sourceCount)
            positions.position(0).limit(vertexCount * 3)
            val count = MeshOptimizer.meshopt_simplify(
                destination,
                source,
                positions,
                vertexCount.toLong(),
                12L,
                targetCount.toLong(),
                0.02f,
                0,
                null
            ).toInt()
            if (count < 3 || count >= sourceCount) {
                IntArray(sourceCount) { source[it] }
            } else {
                destination.position(0).limit(count)
                MeshOptimizer.meshopt_optimizeVertexCache(destination, destination, vertexCount.toLong())
                IntArray(count) { destination[it] }
            }
        } finally {
            MemoryUtil.memFree(destination)
        }
    }
}
