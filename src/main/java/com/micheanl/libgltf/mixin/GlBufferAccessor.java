package com.micheanl.libgltf.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlBuffer")




/**
 * libgltf · GlBufferAccessor
 *
 * ```
 * (slice.buffer() as GlBufferAccessor).`libgltf$handle`(),
 * ```
 *
 * 获取 OpenGL 缓冲句柄的 mixin 访问器
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

public interface GlBufferAccessor {
    @Invoker("handle")
    int libgltf$handle();
}
