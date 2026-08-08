package com.micheanl.libgltf.render.gpu

import com.micheanl.libgltf.model.GltfPrimitive
import com.micheanl.libgltf.model.PrimitiveMode
import com.micheanl.libgltf.model.VertexLayout
import com.micheanl.libgltf.render.GpuBackendType
import com.micheanl.libgltf.render.vulkan.VulkanUsage
import com.mojang.renderpearl.api.pipeline.IndexType
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.device.GpuDevice
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.meshoptimizer.MeshOptimizer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer




/**
 * libgltf · GpuMesh
 *
 * ```
 * private lateinit var primitive: GpuMesh
 * ```
 *
 * 网格的 GPU 资源与 LOD 缓冲
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GpuMesh private constructor(
    val vertexBuffer: GpuBuffer,
    val skinBuffer: GpuBuffer?,
    val indexBuffers: Array<GpuBuffer>,
    val indexType: IndexType,
    val indexCounts: IntArray,
    val indices: Array<IntArray>,
    val triangleCenters: Array<FloatArray>?,
    val sortingPositions: FloatArray?,
    val sortingSkin: ShortArray?,
    val boundsSphere: FloatArray,
    val meshlets: Array<MeshletStorage>?
) : AutoCloseable {
    override fun close() {
        vertexBuffer.close()
        skinBuffer?.close()
        for (buffer in indexBuffers) buffer.close()
        meshlets?.forEach(MeshletStorage::close)
    }

    companion object {
        fun create(device: GpuDevice, label: String, primitive: GltfPrimitive, buildMeshlets: Boolean): GpuMesh {
            val sourceVertices = directCopy(primitive.vertices)
            val sourceSkin = primitive.skin?.let(::directCopy)
            val positions = extractPositions(sourceVertices, primitive.vertexCount)
            val optimized = Array(primitive.lodIndices.size) { level ->
                optimizeIndices(primitive.lodIndices[level], positions, primitive.vertexCount)
            }
            val remap = MemoryUtil.memAllocInt(primitive.vertexCount)
            val remappedVertices = MemoryUtil.memAlloc(primitive.vertexCount * VertexLayout.STRIDE)
            val remappedSkin = sourceSkin?.let { MemoryUtil.memAlloc(primitive.vertexCount * VertexLayout.SKIN_STRIDE) }
            var remappedPositions: FloatBuffer? = null
            var remappedNormals: FloatBuffer? = null
            try {
                optimized[0].position(0)
                MeshOptimizer.meshopt_optimizeVertexFetchRemap(remap, optimized[0])
                MeshOptimizer.meshopt_remapVertexBuffer(
                    remappedVertices,
                    sourceVertices,
                    primitive.vertexCount.toLong(),
                    VertexLayout.STRIDE.toLong(),
                    remap
                )
                if (sourceSkin != null && remappedSkin != null) {
                    MeshOptimizer.meshopt_remapVertexBuffer(
                        remappedSkin,
                        sourceSkin,
                        primitive.vertexCount.toLong(),
                        VertexLayout.SKIN_STRIDE.toLong(),
                        remap
                    )
                }
                for (level in optimized.indices) {
                    val source = optimized[level]
                    val destination = MemoryUtil.memAllocInt(source.remaining())
                    MeshOptimizer.meshopt_remapIndexBuffer(destination, source, source.remaining().toLong(), remap)
                    MemoryUtil.memFree(source)
                    optimized[level] = destination
                }
                remappedPositions = extractPositions(remappedVertices, primitive.vertexCount)
                remappedNormals = extractNormals(remappedVertices, primitive.vertexCount)
                val indexType = IndexType.least((primitive.vertexCount - 1).coerceAtLeast(0))
                val indices = Array(optimized.size) { copyIndices(optimized[it]) }
                val triangleCenters = if (primitive.mode == PrimitiveMode.TRIANGLES) {
                    Array(indices.size) { calculateTriangleCenters(indices[it], remappedPositions) }
                } else {
                    null
                }
                val sortingSkin = if (primitive.mode == PrimitiveMode.TRIANGLES) {
                    remappedSkin?.let { copySkin(it, primitive.vertexCount) }
                } else {
                    null
                }
                val sortingPositions = if (sortingSkin == null) null else copyPositions(remappedPositions)
                val vertexBuffer = device.createBuffer(
                    { "$label vertices" },
                    GpuBuffer.USAGE_VERTEX or if (buildMeshlets) VulkanUsage.STORAGE else 0,
                    remappedVertices.position(0)
                )
                val skinBuffer = remappedSkin?.let {
                    device.createBuffer({ "$label skin" }, GpuBuffer.USAGE_VERTEX, it.position(0))
                }
                val indexBuffers = Array(optimized.size) { level ->
                    val packed = packIndices(optimized[level], indexType)
                    try {
                        device.createBuffer({ "$label lod $level indices" }, GpuBuffer.USAGE_INDEX, packed)
                    } finally {
                        MemoryUtil.memFree(packed)
                    }
                }
                val boundsSphere = boundingSphere(primitive.bounds)
                val meshlets = if (buildMeshlets && primitive.mode == PrimitiveMode.TRIANGLES) {
                    val gl = GpuBackend.capabilities().backend == GpuBackendType.OPENGL
                    Array(optimized.size) { level ->
                        MeshletStorage.create(
                            device,
                            "$label lod $level",
                            optimized[level],
                            remappedPositions,
                            remappedVertices,
                            primitive.vertexCount,
                            indexType,
                            if (gl) 256 else 64,
                            if (gl) 256 else 64,
                            primitive.bounds
                        )
                    }
                } else {
                    null
                }
                return GpuMesh(
                    vertexBuffer,
                    skinBuffer,
                    indexBuffers,
                    indexType,
                    IntArray(optimized.size) { optimized[it].remaining() },
                    indices,
                    triangleCenters,
                    sortingPositions,
                    sortingSkin,
                    boundsSphere,
                    meshlets
                )
            } finally {
                for (buffer in optimized) MemoryUtil.memFree(buffer)
                MemoryUtil.memFree(remappedNormals)
                MemoryUtil.memFree(remappedPositions)
                MemoryUtil.memFree(remappedSkin)
                MemoryUtil.memFree(remappedVertices)
                MemoryUtil.memFree(remap)
                MemoryUtil.memFree(positions)
                MemoryUtil.memFree(sourceSkin)
                MemoryUtil.memFree(sourceVertices)
            }
        }

        private fun boundingSphere(bounds: FloatArray): FloatArray {
            val centerX = (bounds[0] + bounds[3]) * 0.5f
            val centerY = (bounds[1] + bounds[4]) * 0.5f
            val centerZ = (bounds[2] + bounds[5]) * 0.5f
            val extentX = bounds[3] - centerX
            val extentY = bounds[4] - centerY
            val extentZ = bounds[5] - centerZ
            return floatArrayOf(
                centerX,
                centerY,
                centerZ,
                kotlin.math.sqrt(extentX * extentX + extentY * extentY + extentZ * extentZ)
            )
        }
        private fun optimizeIndices(indices: IntArray, positions: FloatBuffer, vertexCount: Int): IntBuffer {
            val source = MemoryUtil.memAllocInt(indices.size)
            val cached = MemoryUtil.memAllocInt(indices.size)
            val overdraw = MemoryUtil.memAllocInt(indices.size)
            source.put(indices).flip()
            positions.position(0)
            MeshOptimizer.meshopt_optimizeVertexCache(cached, source, vertexCount.toLong())
            cached.position(0)
            MeshOptimizer.meshopt_optimizeOverdraw(overdraw, cached, positions, vertexCount.toLong(), 12L, 1.05f)
            MemoryUtil.memFree(cached)
            MemoryUtil.memFree(source)
            return overdraw.position(0)
        }

        private fun directCopy(source: ByteBuffer): ByteBuffer {
            val input = source.duplicate().position(0)
            return MemoryUtil.memAlloc(input.remaining()).put(input).flip()
        }

        private fun extractPositions(vertices: ByteBuffer, vertexCount: Int): FloatBuffer {
            val positions = MemoryUtil.memAllocFloat(vertexCount * 3)
            for (vertex in 0 until vertexCount) {
                val base = vertex * VertexLayout.STRIDE + VertexLayout.POSITION
                positions.put(vertices.getFloat(base))
                positions.put(vertices.getFloat(base + 4))
                positions.put(vertices.getFloat(base + 8))
            }
            return positions.flip()
        }

        private fun extractNormals(vertices: ByteBuffer, vertexCount: Int): FloatBuffer {
            val normals = MemoryUtil.memAllocFloat(vertexCount * 3)
            for (vertex in 0 until vertexCount) {
                val base = vertex * VertexLayout.STRIDE + VertexLayout.NORMAL
                normals.put(vertices.getFloat(base))
                normals.put(vertices.getFloat(base + 4))
                normals.put(vertices.getFloat(base + 8))
            }
            return normals.flip()
        }

        private fun copyPositions(positions: FloatBuffer): FloatArray {
            val result = FloatArray(positions.limit())
            for (index in result.indices) result[index] = positions[index]
            return result
        }

        private fun copySkin(skin: ByteBuffer, vertexCount: Int): ShortArray {
            val result = ShortArray(vertexCount * VertexLayout.SKIN_STRIDE / Short.SIZE_BYTES)
            for (index in result.indices) result[index] = skin.getShort(index * Short.SIZE_BYTES)
            return result
        }

        private fun copyIndices(indices: IntBuffer): IntArray {
            val result = IntArray(indices.limit())
            for (index in result.indices) result[index] = indices[index]
            return result
        }

        private fun calculateTriangleCenters(indices: IntArray, positions: FloatBuffer): FloatArray {
            val centers = FloatArray(indices.size)
            var index = 0
            while (index < indices.size) {
                val first = indices[index] * 3
                val second = indices[index + 1] * 3
                val third = indices[index + 2] * 3
                centers[index] = (positions[first] + positions[second] + positions[third]) / 3.0f
                centers[index + 1] = (positions[first + 1] + positions[second + 1] + positions[third + 1]) / 3.0f
                centers[index + 2] = (positions[first + 2] + positions[second + 2] + positions[third + 2]) / 3.0f
                index += 3
            }
            return centers
        }

        private fun packIndices(indices: IntBuffer, type: IndexType): ByteBuffer {
            val packed = MemoryUtil.memAlloc(indices.remaining() * type.bytes)
            if (type == IndexType.SHORT) {
                for (index in indices.position() until indices.limit()) packed.putShort(indices[index].toShort())
            } else {
                for (index in indices.position() until indices.limit()) packed.putInt(indices[index])
            }
            return packed.flip()
        }
    }
}
