package com.micheanl.libgltf.render.vulkan

/**
 * libgltf · VulkanMeshRenderPass
 *
 * Vulkan mesh 绘制接入
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import com.micheanl.libgltf.render.gpu.MeshletStorage
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.buffers.GpuBuffer

interface VulkanMeshRenderPass {
    fun drawMeshTasks(
        cache: VulkanMeshCache,
        renderPipeline: RenderPipeline,
        geometry: GpuBuffer,
        instances: GpuBuffer,
        meshlets: MeshletStorage,
        sphere: FloatArray,
        instanceCount: Int,
        instanceCulling: Boolean,
        meshletCulling: Boolean
    ): Boolean
}
