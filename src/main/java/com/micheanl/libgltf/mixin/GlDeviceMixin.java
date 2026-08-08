package com.micheanl.libgltf.mixin;

import com.micheanl.libgltf.render.gpu.VertexAttributeLimitProvider;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlDevice")
public abstract class GlDeviceMixin implements VertexAttributeLimitProvider {
    @Override
    public int getMaxVertexAttributes() {
        return GL11.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS);
    }
}
