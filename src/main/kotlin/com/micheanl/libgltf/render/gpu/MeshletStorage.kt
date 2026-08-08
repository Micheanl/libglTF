package com.micheanl.libgltf.render.gpu

import com.micheanl.libgltf.render.vulkan.VulkanUsage
import com.micheanl.libgltf.model.VertexLayout
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

/**
 * libgltf · MeshletStorage
 *
 * ```
 * MeshletStorage meshlets,
 * ```
 *
 * meshlet 的 GPU 缓冲：紧凑顶点、三角形与元数据
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class MeshletStorage private constructor(
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
            attributes: ByteBuffer,
            vertexCount: Int,
            indexType: IndexType,
            maxVertices: Int,
            maxTriangles: Int,
            bounds: FloatArray
        ): MeshletStorage {
            val indexCount = indices.remaining()
            val maxMeshlets = MeshOptimizer.meshopt_buildMeshletsBound(
                indexCount.toLong(), maxVertices.toLong(), maxTriangles.toLong()
            ).toInt()
            val meshlets = MeshoptMeshlet.calloc(maxMeshlets)
            val meshletVertices = MemoryUtil.memAllocInt(maxMeshlets * maxVertices)
            val meshletTriangles = MemoryUtil.memAlloc(maxMeshlets * maxTriangles * 3)
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
                    vertexCount.toLong(), POSITION_STRIDE.toLong(), maxVertices.toLong(),
                    maxTriangles.toLong(), CONE_WEIGHT
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
                vertexData = MemoryUtil.memAlloc(usedVertices * COMPACT_VERTEX_WORDS * Int.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                val compactVertexData = vertexData.asIntBuffer()
                triangleData = MemoryUtil.memCalloc(align4(usedTriangleBytes)).order(ByteOrder.nativeOrder())
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
                        MeshOptimizer.meshopt_optimizeMeshlet(
                            meshletVertices.duplicate().position(vertexOffset)
                                .limit(vertexOffset + meshlet.vertex_count()).slice(),
                            meshletTriangles.duplicate().position(triangleOffset)
                                .limit(triangleOffset + triangleIndexCount).slice()
                        )
                        for (triangleIndex in 0 until triangleIndexCount) {
                            triangleData.put(triangleOffset + triangleIndex, meshletTriangles[triangleOffset + triangleIndex])
                        }
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
                        val cone = coneData(
                            positions,
                            meshletVertices,
                            meshletTriangles,
                            vertexOffset,
                            triangleOffset,
                            triangleCount,
                            meshletBounds.center(0),
                            meshletBounds.center(1),
                            meshletBounds.center(2)
                        )
                        var minX = Float.POSITIVE_INFINITY
                        var minY = Float.POSITIVE_INFINITY
                        var minZ = Float.POSITIVE_INFINITY
                        var maxX = Float.NEGATIVE_INFINITY
                        var maxY = Float.NEGATIVE_INFINITY
                        var maxZ = Float.NEGATIVE_INFINITY
                        for (vertex in 0 until meshlet.vertex_count()) {
                            val positionOffset = meshletVertices[vertexOffset + vertex] * 3
                            val px = positions[positionOffset]
                            val py = positions[positionOffset + 1]
                            val pz = positions[positionOffset + 2]
                            minX = minOf(minX, px)
                            minY = minOf(minY, py)
                            minZ = minOf(minZ, pz)
                            maxX = maxOf(maxX, px)
                            maxY = maxOf(maxY, py)
                            maxZ = maxOf(maxZ, pz)
                        }
                        val extent = maxOf(maxX - minX, maxY - minY, maxZ - minZ)
                        val positionScale = if (extent > 1.0e-6f) extent / 65535.0f else 1.0f
                        for (vertex in 0 until meshlet.vertex_count()) {
                            writeCompactVertex(
                                compactVertexData,
                                (vertexOffset + vertex) * COMPACT_VERTEX_WORDS,
                                positions,
                                attributes,
                                meshletVertices[vertexOffset + vertex],
                                minX,
                                minY,
                                minZ,
                                positionScale
                            )
                        }
                        putMetadata(
                            metadata,
                            meshletBounds.center(0),
                            meshletBounds.center(1),
                            meshletBounds.center(2),
                            meshletBounds.radius(),
                            cone[0],
                            cone[1],
                            cone[2],
                            cone[3],
                            cone[4],
                            cone[5],
                            cone[6],
                            minX,
                            minY,
                            minZ,
                            positionScale,
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
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    0,
                    0,
                    0,
                    0,
                    0,
                    indexCount
                )
                packedIndices.flip()
                metadata.flip()
                vertexData.position(usedVertices * COMPACT_VERTEX_WORDS * Int.SIZE_BYTES).flip()
                triangleData.flip()
                wholeMetadata.flip()
                return MeshletStorage(
                    device.createBuffer({ "$label meshlet indices" }, GpuBuffer.USAGE_INDEX, packedIndices),
                    device.createBuffer({ "$label meshlet metadata" }, VulkanUsage.STORAGE, metadata),
                    device.createBuffer({ "$label meshlet vertices" }, VulkanUsage.STORAGE, vertexData),
                    device.createBuffer({ "$label meshlet triangles" }, VulkanUsage.STORAGE, triangleData),
                    meshletCount,
                    device.createBuffer({ "$label primitive metadata" }, VulkanUsage.STORAGE, wholeMetadata)
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
            coneAxisX: Float,
            coneAxisY: Float,
            coneAxisZ: Float,
            coneCutoff: Float,
            coneApexX: Float,
            coneApexY: Float,
            coneApexZ: Float,
            biasX: Float,
            biasY: Float,
            biasZ: Float,
            biasScale: Float,
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
            buffer.putFloat(coneAxisX).putFloat(coneAxisY).putFloat(coneAxisZ).putFloat(coneCutoff)
            buffer.putFloat(coneApexX).putFloat(coneApexY).putFloat(coneApexZ).putFloat(0.0f)
            buffer.putFloat(biasX).putFloat(biasY).putFloat(biasZ).putFloat(biasScale)
        }

        private fun writeCompactVertex(
            vertexData: IntBuffer,
            wordOffset: Int,
            positions: FloatBuffer,
            attributes: ByteBuffer,
            vertexIndex: Int,
            minX: Float,
            minY: Float,
            minZ: Float,
            scale: Float
        ) {
            val positionOffset = vertexIndex * 3
            val qx = ((positions[positionOffset] - minX) / scale + 0.5f).toInt().coerceIn(0, 65535)
            val qy = ((positions[positionOffset + 1] - minY) / scale + 0.5f).toInt().coerceIn(0, 65535)
            val qz = ((positions[positionOffset + 2] - minZ) / scale + 0.5f).toInt().coerceIn(0, 65535)
            val attributeOffset = vertexIndex * VertexLayout.STRIDE
            val normal = oct16(
                attributes.getFloat(attributeOffset + VertexLayout.NORMAL),
                attributes.getFloat(attributeOffset + VertexLayout.NORMAL + 4),
                attributes.getFloat(attributeOffset + VertexLayout.NORMAL + 8)
            )
            val uv = (java.lang.Float.floatToFloat16(attributes.getFloat(attributeOffset + VertexLayout.UV0)).toInt() and 0xFFFF) or
                ((java.lang.Float.floatToFloat16(attributes.getFloat(attributeOffset + VertexLayout.UV0 + 4)).toInt() and 0xFFFF) shl 16)
            val color = attributes.getInt(attributeOffset + VertexLayout.COLOR)
            vertexData.put(wordOffset, qx or (qy shl 16))
            vertexData.put(wordOffset + 1, qz or (normal shl 16))
            vertexData.put(wordOffset + 2, uv)
            vertexData.put(wordOffset + 3, color)
        }

        private fun oct16(nx: Float, ny: Float, nz: Float): Int {
            var x = nx
            var y = ny
            var z = nz
            val length = kotlin.math.sqrt(x * x + y * y + z * z)
            if (length <= 1.0e-8f) {
                x = 0.0f
                y = 0.0f
                z = 1.0f
            } else {
                x /= length
                y /= length
                z /= length
            }
            val l1 = kotlin.math.abs(x) + kotlin.math.abs(y) + kotlin.math.abs(z)
            var ux = x / l1
            var uy = y / l1
            if (z < 0.0f) {
                val sx = if (ux >= 0.0f) 1.0f else -1.0f
                val sy = if (uy >= 0.0f) 1.0f else -1.0f
                val tx = ux
                val ty = uy
                ux = (1.0f - kotlin.math.abs(ty)) * sx
                uy = (1.0f - kotlin.math.abs(tx)) * sy
            }
            val qx = (ux * 127.0f).toInt().coerceIn(-128, 127)
            val qy = (uy * 127.0f).toInt().coerceIn(-128, 127)
            return (qx and 0xFF) or ((qy and 0xFF) shl 8)
        }

        private fun coneData(
            positions: FloatBuffer,
            meshletVertices: IntBuffer,
            meshletTriangles: ByteBuffer,
            vertexOffset: Int,
            triangleOffset: Int,
            triangleCount: Int,
            centerX: Float,
            centerY: Float,
            centerZ: Float
        ): FloatArray {
            val normals = FloatArray(triangleCount * 3)
            val corners = FloatArray(triangleCount * 3)
            var validCount = 0
            for (triangle in 0 until triangleCount) {
                val triangleBase = triangleOffset + triangle * 3
                val local0 = meshletTriangles[triangleBase].toInt() and 0xFF
                val local1 = meshletTriangles[triangleBase + 1].toInt() and 0xFF
                val local2 = meshletTriangles[triangleBase + 2].toInt() and 0xFF
                val index0 = meshletVertices[vertexOffset + local0] * 3
                val index1 = meshletVertices[vertexOffset + local1] * 3
                val index2 = meshletVertices[vertexOffset + local2] * 3
                val edge1X = positions[index1] - positions[index0]
                val edge1Y = positions[index1 + 1] - positions[index0 + 1]
                val edge1Z = positions[index1 + 2] - positions[index0 + 2]
                val edge2X = positions[index2] - positions[index0]
                val edge2Y = positions[index2 + 1] - positions[index0 + 1]
                val edge2Z = positions[index2 + 2] - positions[index0 + 2]
                var normalX = edge1Y * edge2Z - edge1Z * edge2Y
                var normalY = edge1Z * edge2X - edge1X * edge2Z
                var normalZ = edge1X * edge2Y - edge1Y * edge2X
                val length = kotlin.math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ)
                if (length <= 1.0e-8f) continue
                val offset = validCount * 3
                normalX /= length
                normalY /= length
                normalZ /= length
                normals[offset] = normalX
                normals[offset + 1] = normalY
                normals[offset + 2] = normalZ
                corners[offset] = positions[index0]
                corners[offset + 1] = positions[index0 + 1]
                corners[offset + 2] = positions[index0 + 2]
                validCount++
            }
            if (validCount == 0) return floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f)
            var axisX = 0.0f
            var axisY = 0.0f
            var axisZ = 0.0f
            for (offset in 0 until validCount * 3 step 3) {
                axisX += normals[offset]
                axisY += normals[offset + 1]
                axisZ += normals[offset + 2]
            }
            val axisLength = kotlin.math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
            if (axisLength <= 1.0e-6f) return floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f)
            axisX /= axisLength
            axisY /= axisLength
            axisZ /= axisLength
            var minDot = 1.0f
            for (offset in 0 until validCount * 3 step 3) {
                val dot = axisX * normals[offset] + axisY * normals[offset + 1] + axisZ * normals[offset + 2]
                minDot = minOf(minDot, dot)
            }
            if (minDot <= 0.1f) return floatArrayOf(axisX, axisY, axisZ, 1.0f, 0.0f, 0.0f, 0.0f)
            var maxT = 0.0f
            for (offset in 0 until validCount * 3 step 3) {
                val planeDistance = (centerX - corners[offset]) * normals[offset] +
                    (centerY - corners[offset + 1]) * normals[offset + 1] +
                    (centerZ - corners[offset + 2]) * normals[offset + 2]
                val axisDot = axisX * normals[offset] + axisY * normals[offset + 1] + axisZ * normals[offset + 2]
                maxT = maxOf(maxT, planeDistance / axisDot)
            }
            val cutoff = kotlin.math.sqrt(maxOf(1.0f - minDot * minDot, 0.0f))
            return floatArrayOf(
                axisX,
                axisY,
                axisZ,
                cutoff,
                centerX - axisX * maxT,
                centerY - axisY * maxT,
                centerZ - axisZ * maxT
            )
        }

        private fun align4(value: Int): Int = (value + 3) and -4

        private const val CONE_WEIGHT = 0.25f
        private const val POSITION_STRIDE = 12
        private const val COMPACT_VERTEX_WORDS = 4
        private const val METADATA_STRIDE = 96
    }
}
