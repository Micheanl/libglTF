package com.micheanl.libgltf.mixin;

import com.micheanl.libgltf.render.gpu.VertexAttributeLimitProvider;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlDevice")

/**
 * libgltf · GlDeviceMixin
 *
 * ```
 * @Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlDevice")
 * ```
 *
 * 提供 OpenGL 顶点属性上限的 mixin
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

public abstract class GlDeviceMixin implements VertexAttributeLimitProvider {
    @Override
    public int getMaxVertexAttributes() {
        return GL11.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS);
    }
}
