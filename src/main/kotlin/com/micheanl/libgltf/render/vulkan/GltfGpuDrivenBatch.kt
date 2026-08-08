package com.micheanl.libgltf.render.vulkan

import com.micheanl.libgltf.mixin.FrontendRenderPassAccessor
import com.micheanl.libgltf.render.gpu.GltfGpuPrimitive
import com.micheanl.libgltf.render.gpu.GltfMeshletLod
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.commands.RenderPass
import com.mojang.blaze3d.systems.RenderSystem

class GltfGpuDrivenBatch : AutoCloseable {
    private var commandRing: GltfVulkanBufferRing? = null
    private var statsRing: GltfVulkanBufferRing? = null
    private var commandCapacity = 0
    private var maxDrawCount = 0
    private var meshletCount = 0
    private var metadata: GpuBuffer? = null
    private var meshlets: GltfMeshletLod? = null
    private var indexBuffer: GpuBuffer? = null
    private var active = false
    private var meshletCulling = false
    private var benchmark: GltfVulkanBenchmarkReadback? = null

    fun prepare(primitive: GltfGpuPrimitive, lod: Int, instanceCount: Int, skinned: Boolean, transparent: Boolean, meshShader: Boolean): Boolean {
        val meshletLod = primitive.meshlets?.getOrNull(lod)
        if (skinned || transparent || meshletLod == null) {
            active = false
            return false
        }
        meshlets = meshletLod
        meshletCulling = GltfGpuDrivenSettings.meshletCulling && meshletLod.meshletCount > 1
        meshletCount = if (meshletCulling) meshletLod.meshletCount else 1
        val workloadMeshletCount = if (meshShader) meshletLod.meshletCount else meshletCount
        if (!GltfGpuDrivenSettings.profitable(instanceCount, workloadMeshletCount)) {
            active = false
            return false
        }
        maxDrawCount = instanceCount * meshletCount
        if (!meshShader) ensureCapacity(maxDrawCount * COMMAND_STRIDE)
        metadata = if (meshletCulling) meshletLod.metadataBuffer else meshletLod.wholeMetadataBuffer
        indexBuffer = if (meshletCulling) meshletLod.indexBuffer else primitive.indexBuffers[lod]
        active = true
        return true
    }

    fun dispatch(driver: GltfVulkanGpuDriven, primitive: GltfGpuPrimitive, instances: GpuBuffer, instanceCount: Int): Boolean {
        if (!active) return false
        val projection = RenderSystem.getProjectionMatrixBuffer() ?: return false
        driver.pipeline.dispatch(
            instances,
            requireNotNull(metadata),
            requireNotNull(commandRing).buffer(),
            requireNotNull(statsRing).buffer(),
            projection,
            RenderSystem.getModelViewMatrixCopy(),
            primitive.boundsSphere,
            instanceCount,
            meshletCount,
            GltfGpuDrivenSettings.instanceCulling,
            meshletCulling
        )
        return true
    }

    fun meshReady(driver: GltfVulkanGpuDriven): Boolean = active && driver.meshPipelines?.supported == true

    fun drawMesh(
        renderPass: RenderPass,
        driver: GltfVulkanGpuDriven,
        primitive: GltfGpuPrimitive,
        instances: GpuBuffer,
        instanceCount: Int,
        renderPipeline: RenderPipeline
    ): Boolean {
        if (!active) return false
        val backend = (renderPass as FrontendRenderPassAccessor).libgltfBackend as? VulkanMeshRenderPass ?: return false
        return backend.drawMeshTasks(
            driver.meshPipelines ?: return false,
            renderPipeline,
            primitive.vertexBuffer,
            instances,
            requireNotNull(meshlets),
            primitive.boundsSphere,
            instanceCount,
            GltfGpuDrivenSettings.instanceCulling,
            meshletCulling
        )
    }

    fun captureMesh(nanos: Long, instanceCount: Int, triangleCount: Int, lod: Int) {
        if (!active || !GltfGpuDrivenSettings.benchmark) return
        val candidates = instanceCount * requireNotNull(meshlets).meshletCount
        GltfGpuDrivenBenchmark.record(
            nanos,
            instanceCount,
            instanceCount,
            0,
            0,
            0,
            1,
            triangleCount,
            lod,
            "task_mesh",
            false,
            false,
            (candidates + 31) / 32
        )
    }
    fun indexBuffer(): GpuBuffer = requireNotNull(indexBuffer)

    fun draw(renderPass: RenderPass): Boolean {
        if (!active) return false
        val backend = (renderPass as FrontendRenderPassAccessor).libgltfBackend as? VulkanIndirectRenderPass ?: return false
        backend.drawIndexedIndirectCount(
            requireNotNull(commandRing).buffer().slice(),
            requireNotNull(statsRing).buffer().slice(0L, Int.SIZE_BYTES.toLong()),
            maxDrawCount
        )
        return true
    }

    fun capture(nanos: Long, instanceCount: Int, triangleCount: Int, lod: Int) {
        if (!active || !GltfGpuDrivenSettings.benchmark) return
        if (benchmark == null) benchmark = GltfVulkanBenchmarkReadback()
        requireNotNull(benchmark).capture(requireNotNull(statsRing).buffer(), nanos, instanceCount, meshletCount, triangleCount, lod)
    }

    fun rotate() {
        if (!active) return
        requireNotNull(commandRing).rotate()
        requireNotNull(statsRing).rotate()
    }

    override fun close() {
        benchmark?.close()
        benchmark = null
        commandRing?.close()
        commandRing = null
        statsRing?.close()
        statsRing = null
        commandCapacity = 0
        active = false
    }

    private fun ensureCapacity(required: Int) {
        if (required <= commandCapacity) return
        benchmark?.close()
        benchmark = null
        commandRing?.close()
        statsRing?.close()
        commandCapacity = capacity(required)
        val device = RenderSystem.getDevice()
        commandRing = GltfVulkanBufferRing(
            device,
            "libgltf indirect commands",
            GpuBuffer.USAGE_INDIRECT_PARAMETERS or GltfVulkanUsage.STORAGE,
            commandCapacity
        )
        statsRing = GltfVulkanBufferRing(
            device,
            "libgltf indirect stats",
            GpuBuffer.USAGE_INDIRECT_PARAMETERS or GpuBuffer.USAGE_COPY_SRC or GltfVulkanUsage.STORAGE,
            GltfVulkanComputePipeline.STATS_SIZE
        )
    }

    private fun capacity(required: Int): Int {
        var capacity = 256
        while (capacity < required) capacity = capacity shl 1
        return capacity
    }

    private companion object {
        const val COMMAND_STRIDE = 20
    }
}
