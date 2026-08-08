package com.micheanl.libgltf.render.gpu

import com.micheanl.libgltf.render.vulkan.GltfVulkanUsage
import com.mojang.renderpearl.api.pipeline.IndexType
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.device.GpuDevice
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.meshoptimizer.MeshOptimizer
import org.lwjgl.util.meshoptimizer.MeshoptBounds
import org.lwjgl.util.meshoptimizer.MeshoptMeshlet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class GltfMeshletLod private constructor(
    val indexBuffer: GpuBuffer,
    val metadataBuffer: GpuBuffer,
    val vertexBuffer: GpuBuffer,
    val triangleBuffer: GpuBuffer,
    val meshletCount: Int,
    val wholeMetadataBuffer: GpuBuffer
) : AutoCloseable {
    override fun close() {
        indexBuffer.close()
        metadataBuffer.close()
        vertexBuffer.close()
        triangleBuffer.close()
        wholeMetadataBuffer.close()
    }

    companion object {
        fun create(
            device: GpuDevice,
            label: String,
            indices: IntBuffer,
            positions: FloatBuffer,
            vertexCount: Int,
            indexType: IndexType,
            bounds: FloatArray
        ): GltfMeshletLod {
            val indexCount = indices.remaining()
            val maxMeshlets = MeshOptimizer.meshopt_buildMeshletsBound(
                indexCount.toLong(), MAX_VERTICES.toLong(), MAX_TRIANGLES.toLong()
            ).toInt()
            val meshlets = MeshoptMeshlet.calloc(maxMeshlets)
            val meshletVertices = MemoryUtil.memAllocInt(maxMeshlets * MAX_VERTICES)
            val meshletTriangles = MemoryUtil.memAlloc(maxMeshlets * MAX_TRIANGLES * 3)
            var packedIndices: ByteBuffer? = null
            var metadata: ByteBuffer? = null
            var vertexData: ByteBuffer? = null
            var triangleData: ByteBuffer? = null
            var wholeMetadata: ByteBuffer? = null
            try {
                positions.position(0)
                indices.position(0)
                val meshletCount = MeshOptimizer.meshopt_buildMeshlets(
                    meshlets, meshletVertices, meshletTriangles, indices, positions,
                    vertexCount.toLong(), POSITION_STRIDE.toLong(), MAX_VERTICES.toLong(),
                    MAX_TRIANGLES.toLong(), 0.0f
                ).toInt()
                var meshletIndexCount = 0
                var usedVertices = 0
                var usedTriangleBytes = 0
                for (index in 0 until meshletCount) {
                    val meshlet = meshlets[index]
                    meshletIndexCount += meshlet.triangle_count() * 3
                    usedVertices = maxOf(usedVertices, meshlet.vertex_offset() + meshlet.vertex_count())
                    usedTriangleBytes = maxOf(usedTriangleBytes, meshlet.triangle_offset() + meshlet.triangle_count() * 3)
                }
                packedIndices = MemoryUtil.memAlloc(meshletIndexCount * indexType.bytes).order(ByteOrder.nativeOrder())
                metadata = MemoryUtil.memAlloc(meshletCount * METADATA_STRIDE).order(ByteOrder.nativeOrder())
                vertexData = MemoryUtil.memAlloc(usedVertices * Int.SIZE_BYTES).order(ByteOrder.nativeOrder())
                for (index in 0 until usedVertices) vertexData.putInt(meshletVertices[index])
                triangleData = MemoryUtil.memCalloc(align4(usedTriangleBytes)).order(ByteOrder.nativeOrder())
                for (index in 0 until usedTriangleBytes) triangleData.put(index, meshletTriangles[index])
                triangleData.position(triangleData.capacity())
                val meshletBounds = MeshoptBounds.calloc()
                try {
                    var firstIndex = 0
                    for (index in 0 until meshletCount) {
                        val meshlet = meshlets[index]
                        val vertexOffset = meshlet.vertex_offset()
                        val triangleOffset = meshlet.triangle_offset()
                        val triangleCount = meshlet.triangle_count()
                        val triangleIndexCount = triangleCount * 3
                        for (triangleIndex in 0 until triangleIndexCount) {
                            val localIndex = meshletTriangles[triangleOffset + triangleIndex].toInt() and 0xFF
                            putIndex(packedIndices, indexType, meshletVertices[vertexOffset + localIndex])
                        }
                        val vertices = meshletVertices.duplicate().position(vertexOffset)
                            .limit(vertexOffset + meshlet.vertex_count()).slice()
                        val triangles = meshletTriangles.duplicate().position(triangleOffset)
                            .limit(triangleOffset + triangleIndexCount).slice()
                        positions.position(0)
                        MeshOptimizer.meshopt_computeMeshletBounds(
                            vertices, triangles, positions, vertexCount.toLong(), POSITION_STRIDE.toLong(), meshletBounds
                        )
                        putMetadata(
                            metadata,
                            meshletBounds.center(0),
                            meshletBounds.center(1),
                            meshletBounds.center(2),
                            meshletBounds.radius(),
                            vertexOffset,
                            triangleOffset,
                            meshlet.vertex_count(),
                            triangleCount,
                            firstIndex,
                            triangleIndexCount
                        )
                        firstIndex += triangleIndexCount
                    }
                } finally {
                    meshletBounds.free()
                }
                wholeMetadata = MemoryUtil.memAlloc(METADATA_STRIDE).order(ByteOrder.nativeOrder())
                val centerX = (bounds[0] + bounds[3]) * 0.5f
                val centerY = (bounds[1] + bounds[4]) * 0.5f
                val centerZ = (bounds[2] + bounds[5]) * 0.5f
                val extentX = bounds[3] - centerX
                val extentY = bounds[4] - centerY
                val extentZ = bounds[5] - centerZ
                putMetadata(
                    wholeMetadata,
                    centerX,
                    centerY,
                    centerZ,
                    kotlin.math.sqrt(extentX * extentX + extentY * extentY + extentZ * extentZ),
                    0,
                    0,
                    0,
                    0,
                    0,
                    indexCount
                )
                packedIndices.flip()
                metadata.flip()
                vertexData.flip()
                triangleData.flip()
                wholeMetadata.flip()
                return GltfMeshletLod(
                    device.createBuffer({ "$label meshlet indices" }, GpuBuffer.USAGE_INDEX, packedIndices),
                    device.createBuffer({ "$label meshlet metadata" }, GltfVulkanUsage.STORAGE, metadata),
                    device.createBuffer({ "$label meshlet vertices" }, GltfVulkanUsage.STORAGE, vertexData),
                    device.createBuffer({ "$label meshlet triangles" }, GltfVulkanUsage.STORAGE, triangleData),
                    meshletCount,
                    device.createBuffer({ "$label primitive metadata" }, GltfVulkanUsage.STORAGE, wholeMetadata)
                )
            } finally {
                MemoryUtil.memFree(wholeMetadata)
                MemoryUtil.memFree(triangleData)
                MemoryUtil.memFree(vertexData)
                MemoryUtil.memFree(metadata)
                MemoryUtil.memFree(packedIndices)
                MemoryUtil.memFree(meshletTriangles)
                MemoryUtil.memFree(meshletVertices)
                meshlets.free()
            }
        }

        private fun putIndex(buffer: ByteBuffer, indexType: IndexType, value: Int) {
            if (indexType == IndexType.SHORT) buffer.putShort(value.toShort()) else buffer.putInt(value)
        }

        private fun putMetadata(
            buffer: ByteBuffer,
            centerX: Float,
            centerY: Float,
            centerZ: Float,
            radius: Float,
            vertexOffset: Int,
            triangleOffset: Int,
            vertexCount: Int,
            triangleCount: Int,
            firstIndex: Int,
            indexCount: Int
        ) {
            buffer.putFloat(centerX).putFloat(centerY).putFloat(centerZ).putFloat(radius)
            buffer.putInt(vertexOffset).putInt(triangleOffset).putInt(vertexCount).putInt(triangleCount)
            buffer.putInt(firstIndex).putInt(indexCount).putLong(0L)
        }

        private fun align4(value: Int): Int = (value + 3) and -4

        private const val MAX_VERTICES = 256
        private const val MAX_TRIANGLES = 256
        private const val POSITION_STRIDE = 12
        private const val METADATA_STRIDE = 48
    }
}
