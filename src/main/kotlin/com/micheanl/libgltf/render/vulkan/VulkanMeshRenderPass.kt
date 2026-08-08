package com.micheanl.libgltf.render.vulkan

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
