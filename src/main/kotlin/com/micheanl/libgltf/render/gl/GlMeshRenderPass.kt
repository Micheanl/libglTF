package com.micheanl.libgltf.render.gl

import com.micheanl.libgltf.render.gpu.MeshletStorage
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import net.minecraft.client.renderer.rendertype.PreparedRenderType

interface GlMeshRenderPass {
    fun drawMeshTasks(
        cache: GlMeshPipelineCache,
        renderPipeline: RenderPipeline,
        preparedRenderType: PreparedRenderType,
        geometry: GpuBuffer,
        instances: GpuBuffer,
        meshlets: MeshletStorage,
        sphere: FloatArray,
        instanceCount: Int,
        instanceCulling: Boolean,
        meshletCulling: Boolean
    ): Boolean
}
