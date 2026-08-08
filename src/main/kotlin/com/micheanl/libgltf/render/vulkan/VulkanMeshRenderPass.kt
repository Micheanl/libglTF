package com.micheanl.libgltf.render.vulkan

import com.micheanl.libgltf.render.gpu.GltfMeshletLod
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.buffers.GpuBuffer

interface VulkanMeshRenderPass {
    fun drawMeshTasks(
        cache: GltfVulkanMeshCache,
        renderPipeline: RenderPipeline,
        geometry: GpuBuffer,
        instances: GpuBuffer,
        meshlets: GltfMeshletLod,
        sphere: FloatArray,
        instanceCount: Int,
        instanceCulling: Boolean,
        meshletCulling: Boolean
    ): Boolean
}
