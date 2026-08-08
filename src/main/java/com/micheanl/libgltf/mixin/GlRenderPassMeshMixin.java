package com.micheanl.libgltf.mixin;

import com.micheanl.libgltf.render.gl.GlMeshRenderPass;
import com.micheanl.libgltf.render.gl.GlMeshPipelineCache;
import com.micheanl.libgltf.render.gpu.MeshletStorage;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.backend.opengl.GlRenderPipeline;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlRenderPass")




/**
 * libgltf · GlRenderPassMeshMixin
 *
 * ```
 * @Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlRenderPass")
 * ```
 *
 * OpenGL 渲染通道 mesh 绘制接入 mixin
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

public abstract class GlRenderPassMeshMixin implements GlMeshRenderPass {
    @Shadow
    protected GlRenderPipeline pipeline;

    @Override
    public boolean drawMeshTasks(
            GlMeshPipelineCache cache,
            RenderPipeline renderPipeline,
            PreparedRenderType preparedRenderType,
            GpuBuffer geometry,
            GpuBuffer instances,
            MeshletStorage meshlets,
            float[] sphere,
            int instanceCount,
            boolean instanceCulling,
            boolean meshletCulling
    ) {
        if (pipeline != null) {
            pipeline.bind();
        }
        return cache.draw(
                renderPipeline,
                preparedRenderType,
                geometry,
                instances,
                meshlets,
                sphere,
                instanceCount,
                instanceCulling,
                meshletCulling
        );
    }
}
