package com.micheanl.libgltf.render.feature

import com.micheanl.libgltf.render.GltfGpuBackendType
import com.micheanl.libgltf.render.gpu.GltfGpuBackend
import com.micheanl.libgltf.render.gpu.GltfGpuFormats
import com.micheanl.libgltf.render.gpu.GltfGpuPrimitive
import com.micheanl.libgltf.render.vulkan.GltfGpuDrivenBatch
import com.micheanl.libgltf.render.vulkan.GltfGpuDrivenBenchmark
import com.micheanl.libgltf.render.vulkan.GltfGpuDrivenSettings
import com.micheanl.libgltf.render.vulkan.GltfVulkanGpuDriven
import com.micheanl.libgltf.render.vulkan.GltfVulkanUsage
import com.mojang.blaze3d.IndexType
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.CompactVectorArray
import com.mojang.logging.LogUtils
import net.minecraft.client.renderer.MappableRingBuffer
import net.minecraft.client.renderer.rendertype.PreparedRenderType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Optional
import java.util.OptionalDouble

class GltfPreparedGpuBatch : AutoCloseable {
    private var instanceBuffer: MappableRingBuffer? = null
    private var paletteBuffer: MappableRingBuffer? = null
    private var sortedIndexBuffer: MappableRingBuffer? = null
    private val gpuDriven = GltfGpuDrivenBatch()
    private var gpuDrivenDriver: GltfVulkanGpuDriven? = null
    private var storageInstances = false
    private var instanceCapacity = 0
    private var paletteCapacity = 0
    private var sortedIndexCapacity = 0
    private var sortingCenters: CompactVectorArray? = null
    private val sortingPosition = FloatArray(3)
    private lateinit var preparedRenderType: PreparedRenderType
    private lateinit var primitive: GltfGpuPrimitive
    private var lod = 0
    private var instanceCount = 0
    private var skinned = false
    private var useSortedIndexBuffer = false
    private var active = false

    fun prepare(submits: List<GltfGpuSubmit>, fromIndex: Int, toIndex: Int, driver: GltfVulkanGpuDriven?) {
        val first = submits[fromIndex]
        gpuDrivenDriver = driver
        val gpu = first.resource.gpu()
        try {
            primitive = gpu.primitive(first.meshIndex, first.primitiveIndex) ?: run {
                active = false
                return
            }
            preparedRenderType = first.renderType.prepare()
            lod = first.lod
            instanceCount = toIndex - fromIndex
            skinned = first.skinIndex >= 0
            ensureInstanceCapacity(instanceCount * GltfGpuFormats.INSTANCE_STRIDE, driver != null)
            writeInstances(submits, fromIndex, toIndex)
            if (skinned) writePalettes(submits, fromIndex, toIndex)
            useSortedIndexBuffer = first.renderType.hasBlending() && primitive.triangleCenters != null
            if (useSortedIndexBuffer) prepareSortedIndices(first)
            gpuDriven.prepare(
                primitive,
                lod,
                instanceCount,
                skinned,
                first.renderType.hasBlending(),
                driver?.meshPipelines?.supported == true
            )
            active = true
        } catch (error: RuntimeException) {
            gpu.disable(first.meshIndex, first.primitiveIndex)
            active = false
            LOGGER.error("libgltf GPU batch preparation failed", error)
        }
    }

    fun execute() {
        if (!active) return
        val benchmarkStart = if (GltfGpuDrivenSettings.benchmark && GltfGpuBackend.capabilities().backend == GltfGpuBackendType.VULKAN) System.nanoTime() else 0L
        val renderTarget = preparedRenderType.outputTarget().renderTarget
        val colorTexture = requireNotNull(RenderSystem.outputColorTextureOverride ?: renderTarget.colorTextureView)
        val depthTexture = if (renderTarget.useDepth) RenderSystem.outputDepthTextureOverride ?: renderTarget.depthTextureView else null
        val instances = requireNotNull(instanceBuffer)
        val instanceGpuBuffer = instances.currentBuffer()
        val palettes = if (skinned) requireNotNull(paletteBuffer) else null
        val sortedIndices = if (useSortedIndexBuffer) requireNotNull(sortedIndexBuffer) else null
        val meshRequested = gpuDrivenDriver?.let(gpuDriven::meshReady) == true
        val indirect = !meshRequested && gpuDrivenDriver?.let { gpuDriven.dispatch(it, primitive, instanceGpuBuffer, instanceCount) } == true
        var meshDrawn = false
        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "libgltf gpu batch" }, colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty()
        ).use { renderPass ->
            renderPass.setPipeline(preparedRenderType.pipeline())
            val scissor = preparedRenderType.scissorState()
            if (scissor.enabled()) renderPass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height())
            RenderSystem.bindDefaultUniforms(renderPass)
            renderPass.setUniform("DynamicTransforms", preparedRenderType.dynamicTransforms())
            renderPass.setVertexBuffer(0, primitive.vertexBuffer.slice())
            renderPass.setVertexBuffer(1, instanceGpuBuffer.slice())
            if (palettes != null) {
                renderPass.setVertexBuffer(2, requireNotNull(primitive.skinBuffer).slice())
                renderPass.setUniform("JointMatrices", palettes.currentBuffer().slice())
            }
            for (texture in preparedRenderType.textures()) renderPass.bindTexture(texture.name(), texture.textureView(), texture.sampler())
            if (meshRequested) {
                meshDrawn = gpuDriven.drawMesh(renderPass, requireNotNull(gpuDrivenDriver), primitive, instanceGpuBuffer, instanceCount)
                if (!meshDrawn) {
                    renderPass.setIndexBuffer(primitive.indexBuffers[lod], primitive.indexType)
                    renderPass.drawIndexed(primitive.indexCounts[lod], instanceCount, 0, 0, 0)
                }
            } else if (indirect) {
                renderPass.setIndexBuffer(gpuDriven.indexBuffer(), primitive.indexType)
                if (!gpuDriven.draw(renderPass)) {
                    renderPass.setIndexBuffer(primitive.indexBuffers[lod], primitive.indexType)
                    renderPass.drawIndexed(primitive.indexCounts[lod], instanceCount, 0, 0, 0)
                }
            } else {
                val indexBuffer = sortedIndices?.currentBuffer() ?: primitive.indexBuffers[lod]
                renderPass.setIndexBuffer(indexBuffer, primitive.indexType)
                renderPass.drawIndexed(primitive.indexCounts[lod], instanceCount, 0, 0, 0)
            }
        }
        if (benchmarkStart != 0L) {
            val elapsed = System.nanoTime() - benchmarkStart
            if (meshDrawn) {
                gpuDriven.captureMesh(elapsed, instanceCount, primitive.indexCounts[lod] / 3, lod)
            } else if (indirect) {
                gpuDriven.capture(elapsed, instanceCount, primitive.indexCounts[lod] / 3, lod)
            } else {
                GltfGpuDrivenBenchmark.record(
                    elapsed, instanceCount, instanceCount, 0, instanceCount, 0, 1,
                    primitive.indexCounts[lod] / 3, lod, "direct", skinned, useSortedIndexBuffer
                )
            }
        }
        instances.rotate()
        palettes?.rotate()
        sortedIndices?.rotate()
        if (indirect) gpuDriven.rotate()
    }
    override fun close() {
        instanceBuffer?.close()
        instanceBuffer = null
        paletteBuffer?.close()
        paletteBuffer = null
        gpuDriven.close()
        sortedIndexBuffer?.close()
        sortedIndexBuffer = null
        instanceCapacity = 0
        storageInstances = false
        paletteCapacity = 0
        sortedIndexCapacity = 0
        sortingCenters = null
        useSortedIndexBuffer = false
        active = false
    }

    private fun prepareSortedIndices(submit: GltfGpuSubmit) {
        val indices = primitive.indices[lod]
        val cachedCenters = requireNotNull(primitive.triangleCenters)[lod]
        val triangleCount = indices.size / 3
        var centers = sortingCenters
        if (centers == null || centers.size() != triangleCount) {
            centers = CompactVectorArray(triangleCount)
            sortingCenters = centers
        }
        val skin = primitive.sortingSkin
        val palette = if (skin != null && submit.skinIndex >= 0) {
            submit.instance.animationState.jointPalettes[submit.skinIndex]
        } else {
            null
        }
        val positions = primitive.sortingPositions
        val matrix = submit.modelMatrix
        for (triangle in 0 until triangleCount) {
            val indexOffset = triangle * 3
            var centerX = 0.0f
            var centerY = 0.0f
            var centerZ = 0.0f
            if (skin == null || palette == null) {
                centerX = cachedCenters[indexOffset]
                centerY = cachedCenters[indexOffset + 1]
                centerZ = cachedCenters[indexOffset + 2]
            } else {
                for (corner in 0 until 3) {
                    val vertex = indices[indexOffset + corner]
                    skinPosition(vertex, requireNotNull(positions), skin, palette)
                    centerX += sortingPosition[0]
                    centerY += sortingPosition[1]
                    centerZ += sortingPosition[2]
                }
                centerX /= 3.0f
                centerY /= 3.0f
                centerZ /= 3.0f
            }
            val x = matrix.m00() * centerX + matrix.m10() * centerY + matrix.m20() * centerZ + matrix.m30()
            val y = matrix.m01() * centerX + matrix.m11() * centerY + matrix.m21() * centerZ + matrix.m31()
            val z = matrix.m02() * centerX + matrix.m12() * centerY + matrix.m22() * centerZ + matrix.m32()
            centers.set(triangle, x, y, z)
        }
        val triangleOrder = RenderSystem.getProjectionType().vertexSorting().sort(centers)
        val byteCount = indices.size * primitive.indexType.bytes
        ensureSortedIndexCapacity(byteCount)
        val view = requireNotNull(sortedIndexBuffer).currentBuffer().slice(0L, byteCount.toLong()).map(false, true)
        view.use { mapped ->
            val data = mapped.data().order(ByteOrder.nativeOrder())
            if (primitive.indexType == IndexType.SHORT) {
                for (triangle in triangleOrder) {
                    val source = triangle * 3
                    data.putShort(indices[source].toShort())
                    data.putShort(indices[source + 1].toShort())
                    data.putShort(indices[source + 2].toShort())
                }
            } else {
                for (triangle in triangleOrder) {
                    val source = triangle * 3
                    data.putInt(indices[source])
                    data.putInt(indices[source + 1])
                    data.putInt(indices[source + 2])
                }
            }
        }
    }

    private fun skinPosition(vertex: Int, positions: FloatArray, skin: ShortArray, palette: FloatArray) {
        val positionOffset = vertex * 3
        val sourceX = positions[positionOffset]
        val sourceY = positions[positionOffset + 1]
        val sourceZ = positions[positionOffset + 2]
        val skinOffset = vertex * 8
        var x = 0.0f
        var y = 0.0f
        var z = 0.0f
        var totalWeight = 0.0f
        for (component in 0 until 4) {
            val weight = (skin[skinOffset + 4 + component].toInt() and 0xFFFF) / 65535.0f
            if (weight <= 0.0f) continue
            val offset = (skin[skinOffset + component].toInt() and 0xFFFF) * 16
            if (offset + 15 >= palette.size) continue
            x += (palette[offset] * sourceX + palette[offset + 4] * sourceY + palette[offset + 8] * sourceZ + palette[offset + 12]) * weight
            y += (palette[offset + 1] * sourceX + palette[offset + 5] * sourceY + palette[offset + 9] * sourceZ + palette[offset + 13]) * weight
            z += (palette[offset + 2] * sourceX + palette[offset + 6] * sourceY + palette[offset + 10] * sourceZ + palette[offset + 14]) * weight
            totalWeight += weight
        }
        sortingPosition[0] = if (totalWeight > 0.0f) x else sourceX
        sortingPosition[1] = if (totalWeight > 0.0f) y else sourceY
        sortingPosition[2] = if (totalWeight > 0.0f) z else sourceZ
    }

    private fun writeInstances(submits: List<GltfGpuSubmit>, fromIndex: Int, toIndex: Int) {
        val view = requireNotNull(instanceBuffer)
            .currentBuffer()
            .slice(0L, (instanceCount * GltfGpuFormats.INSTANCE_STRIDE).toLong())
            .map(false, true)
        view.use { mapped ->
            val data = mapped.data().order(ByteOrder.nativeOrder())
            var paletteOffset = 0
            var destinationIndex = 0
            for (index in fromIndex until toIndex) {
                val submit = submits[index]
                writeInstance(data, destinationIndex * GltfGpuFormats.INSTANCE_STRIDE, submit, paletteOffset)
                if (skinned) paletteOffset += submit.instance.animationState.jointPalettes[submit.skinIndex].size / 4
                destinationIndex++
            }
        }
    }

    private fun writeInstance(data: ByteBuffer, offset: Int, submit: GltfGpuSubmit, paletteOffset: Int) {
        submit.modelMatrix.get(offset, data)
        val normal = submit.normalMatrix
        data.putFloat(offset + 64, normal.m00())
        data.putFloat(offset + 68, normal.m01())
        data.putFloat(offset + 72, normal.m02())
        data.putFloat(offset + 76, 0.0f)
        data.putFloat(offset + 80, normal.m10())
        data.putFloat(offset + 84, normal.m11())
        data.putFloat(offset + 88, normal.m12())
        data.putFloat(offset + 92, 0.0f)
        data.putFloat(offset + 96, normal.m20())
        data.putFloat(offset + 100, normal.m21())
        data.putFloat(offset + 104, normal.m22())
        data.putFloat(offset + 108, 0.0f)
        data.putFloat(offset + 112, submit.red)
        data.putFloat(offset + 116, submit.green)
        data.putFloat(offset + 120, submit.blue)
        data.putFloat(offset + 124, submit.alpha)
        data.putInt(offset + 128, submit.light)
        data.putInt(offset + 132, submit.overlay)
        data.putInt(offset + 136, paletteOffset)
    }

    private fun writePalettes(submits: List<GltfGpuSubmit>, fromIndex: Int, toIndex: Int) {
        var floatCount = 0
        for (index in fromIndex until toIndex) {
            val submit = submits[index]
            floatCount += submit.instance.animationState.jointPalettes[submit.skinIndex].size
        }
        val byteCount = floatCount * Float.SIZE_BYTES
        ensurePaletteCapacity(byteCount)
        val view = requireNotNull(paletteBuffer).currentBuffer().slice(0L, byteCount.toLong()).map(false, true)
        view.use { mapped ->
            val data = mapped.data().order(ByteOrder.nativeOrder()).asFloatBuffer()
            for (index in fromIndex until toIndex) {
                val submit = submits[index]
                data.put(submit.instance.animationState.jointPalettes[submit.skinIndex])
            }
        }
    }

    private fun ensureInstanceCapacity(required: Int, storage: Boolean) {
        if (required <= instanceCapacity && storage == storageInstances) return
        instanceBuffer?.close()
        instanceCapacity = capacity(required)
        storageInstances = storage
        val usage = GpuBuffer.USAGE_MAP_WRITE or
            GpuBuffer.USAGE_HINT_CLIENT_STORAGE or
            GpuBuffer.USAGE_VERTEX or
            if (storage) GltfVulkanUsage.STORAGE else 0
        instanceBuffer = MappableRingBuffer({ "libgltf instance buffer" }, usage, instanceCapacity)
    }
    private fun ensurePaletteCapacity(required: Int) {
        if (required <= paletteCapacity) return
        paletteBuffer?.close()
        paletteCapacity = capacity(required)
        paletteBuffer = MappableRingBuffer(
            { "libgltf joint palette buffer" },
            GpuBuffer.USAGE_MAP_WRITE or GpuBuffer.USAGE_HINT_CLIENT_STORAGE or GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER,
            paletteCapacity
        )
    }

    private fun ensureSortedIndexCapacity(required: Int) {
        if (required <= sortedIndexCapacity) return
        sortedIndexBuffer?.close()
        sortedIndexCapacity = capacity(required)
        sortedIndexBuffer = MappableRingBuffer(
            { "libgltf sorted index buffer" },
            GpuBuffer.USAGE_MAP_WRITE or GpuBuffer.USAGE_HINT_CLIENT_STORAGE or GpuBuffer.USAGE_INDEX,
            sortedIndexCapacity
        )
    }


    private fun capacity(required: Int): Int {
        var capacity = 256
        while (capacity < required) capacity = capacity shl 1
        return capacity
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
    }
}
