package com.micheanl.libgltf.render.vulkan

import com.micheanl.libgltf.mixin.FrontendRenderPassAccessor
import com.micheanl.libgltf.render.gl.GlMeshRenderPass
import com.micheanl.libgltf.render.gl.GltfGlGpuDriven
import com.micheanl.libgltf.render.gpu.GltfGpuBackend
import com.micheanl.libgltf.render.gpu.GltfGpuDriver
import com.micheanl.libgltf.render.gpu.GltfGpuPrimitive
import com.micheanl.libgltf.render.gpu.GltfMeshletLod
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.commands.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.PreparedRenderType

class GltfGpuDrivenBatch : AutoCloseable {
    private var commandRing: GltfVulkanBufferRing? = null
    private var statsRing: GltfVulkanBufferRing? = null
    private var commandCapacity = 0
    private var maxDrawCount = 0
    private var meshletCount = 0
    private var useMeshlets = false
    private var metadata: GpuBuffer? = null
    private var meshlets: GltfMeshletLod? = null
    private var indexBuffer: GpuBuffer? = null
    private var active = false
    private var meshletCulling = false

    fun prepare(
        primitive: GltfGpuPrimitive,
        lod: Int,
        instanceCount: Int,
        skinned: Boolean,
        transparent: Boolean,
        meshShader: Boolean,
        driver: GltfGpuDriver?
    ): Boolean {
        val meshletLod = primitive.meshlets?.getOrNull(lod)
        val meshOit = Minecraft.getInstance().gameRenderer.useImprovedTransparency() &&
            (driver is GltfVulkanGpuDriven || driver is GltfGlGpuDriven)
        if (skinned || (transparent && !meshOit) || meshletLod == null) {
            active = false
            return false
        }
        meshlets = meshletLod
        val profile = GltfGpuBackend.vendorProfile()
        useMeshlets = profile.enableMeshletCulling && meshletLod.meshletCount > 1
        meshletCulling = useMeshlets && GltfGpuDrivenSettings.meshletCulling
        meshletCount = if (meshletCulling) meshletLod.meshletCount else 1
        val workloadMeshletCount = if (meshShader) meshletLod.meshletCount else meshletCount
        if (!GltfGpuDrivenSettings.profitable(instanceCount, workloadMeshletCount)) {
            active = false
            return false
        }
        maxDrawCount = instanceCount * meshletCount
        if (!(meshShader && useMeshlets)) ensureCapacity(maxDrawCount * COMMAND_STRIDE)
        metadata = if (meshletCulling) meshletLod.metadataBuffer else meshletLod.wholeMetadataBuffer
        indexBuffer = if (meshletCulling) meshletLod.indexBuffer else primitive.indexBuffers[lod]
        active = true
        return true
    }

    fun dispatch(driver: GltfGpuDriver, primitive: GltfGpuPrimitive, instances: GpuBuffer, instanceCount: Int): Boolean {
        if (!active) return false
        if (driver !is GltfVulkanGpuDriven) return false
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
            GltfGpuBackend.vendorProfile().enableInstanceCulling && GltfGpuDrivenSettings.instanceCulling,
            meshletCulling
        )
        return true
    }

    fun meshReady(driver: GltfGpuDriver): Boolean = active && useMeshlets && driver.meshSupported

    fun currentMeshletCount(): Int = meshletCount

    fun drawMesh(
        renderPass: RenderPass,
        driver: GltfGpuDriver,
        primitive: GltfGpuPrimitive,
        instances: GpuBuffer,
        instanceCount: Int,
        renderPipeline: RenderPipeline,
        preparedRenderType: PreparedRenderType
    ): Boolean {
        if (!active) return false
        val instanceCulling = GltfGpuBackend.vendorProfile().enableInstanceCulling && GltfGpuDrivenSettings.instanceCulling
        val backend = (renderPass as FrontendRenderPassAccessor).libgltfBackend
        return when (driver) {
            is GltfVulkanGpuDriven -> (backend as? VulkanMeshRenderPass)?.drawMeshTasks(
                driver.meshPipelines ?: return false,
                renderPipeline,
                primitive.vertexBuffer,
                instances,
                requireNotNull(meshlets),
                primitive.boundsSphere,
                instanceCount,
                instanceCulling,
                meshletCulling
            ) == true
            is GltfGlGpuDriven -> (backend as? GlMeshRenderPass)?.drawMeshTasks(
                driver.meshPipelines ?: return false,
                renderPipeline,
                preparedRenderType,
                primitive.vertexBuffer,
                instances,
                requireNotNull(meshlets),
                primitive.boundsSphere,
                instanceCount,
                instanceCulling,
                meshletCulling
            ) == true
            else -> false
        }
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

    fun rotate() {
        if (!active) return
        requireNotNull(commandRing).rotate()
        requireNotNull(statsRing).rotate()
    }

    override fun close() {
        commandRing?.close()
        commandRing = null
        statsRing?.close()
        statsRing = null
        commandCapacity = 0
        active = false
    }

    private fun ensureCapacity(required: Int) {
        if (required <= commandCapacity) return
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
