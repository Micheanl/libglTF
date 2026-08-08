package com.micheanl.libgltf.mixin;

/**
 * libgltf · GlSamplerAccessor
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlSampler")
public interface GlSamplerAccessor {
    @Invoker("getId")
    int libgltf$getId();
}
