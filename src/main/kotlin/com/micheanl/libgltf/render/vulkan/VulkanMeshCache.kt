package com.micheanl.libgltf.render.vulkan

import com.micheanl.libgltf.render.gpu.MeshletStorage
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline
import org.lwjgl.vulkan.VkCommandBuffer


/**
 * libgltf · VulkanMeshCache
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

interface VulkanMeshCache : AutoCloseable {
    val supported: Boolean

    fun descriptorPipeline(renderPipeline: RenderPipeline, original: VulkanRenderPipeline): VulkanRenderPipeline?

    fun draw(
        renderPipeline: RenderPipeline,
        original: VulkanRenderPipeline,
        commandBuffer: VkCommandBuffer,
        hasDepth: Boolean,
        geometry: GpuBuffer,
        instances: GpuBuffer,
        meshlets: MeshletStorage,
        sphere: FloatArray,
        instanceCount: Int,
        instanceCulling: Boolean,
        meshletCulling: Boolean
    ): Boolean
}
