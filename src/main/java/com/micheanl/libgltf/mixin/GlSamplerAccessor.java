package com.micheanl.libgltf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlSampler")

/**
 * libgltf · GlSamplerAccessor
 *
 * ```
 * GL33C.glBindSampler(unit, (texture.sampler as GlSamplerAccessor).`libgltf$getId`())
 * ```
 *
 * 获取 OpenGL 采样器句柄的 mixin 访问器
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

public interface GlSamplerAccessor {
    @Invoker("getId")
    int libgltf$getId();
}
