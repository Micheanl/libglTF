package com.micheanl.libgltf.mixin;

import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FrontendRenderPass.class)

/**
 * libgltf · FrontendRenderPassAccessor
 *
 * ```
 * val backend = (renderPass as FrontendRenderPassAccessor).libgltfBackend
 * ```
 *
 * 访问前端渲染通道后端实例的 mixin 访问器
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

public interface FrontendRenderPassAccessor {
    @Accessor("backend")
    RenderPassBackend getLibgltfBackend();
}
