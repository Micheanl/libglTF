package com.micheanl.libgltf.render.vulkan

import com.micheanl.libgltf.render.gpu.GltfMeshletStorage
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline
import org.lwjgl.vulkan.VkCommandBuffer

interface GltfVulkanMeshCache : AutoCloseable {
    val supported: Boolean

    fun descriptorPipeline(renderPipeline: RenderPipeline, original: VulkanRenderPipeline): VulkanRenderPipeline?

    fun draw(
        renderPipeline: RenderPipeline,
        original: VulkanRenderPipeline,
        commandBuffer: VkCommandBuffer,
        hasDepth: Boolean,
        geometry: GpuBuffer,
        instances: GpuBuffer,
        meshlets: GltfMeshletStorage,
        sphere: FloatArray,
        instanceCount: Int,
        instanceCulling: Boolean,
        meshletCulling: Boolean
    ): Boolean
}
