package com.micheanl.libgltf.mixin;

/**
 * libgltf · GlBufferAccessor
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlBuffer")
public interface GlBufferAccessor {
    @Invoker("handle")
    int libgltf$handle();
}
