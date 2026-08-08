package com.micheanl.libgltf.render.iris

import com.mojang.renderpearl.backend.opengl.GlTexture
import net.irisshaders.iris.api.v0.IrisApi
import net.minecraft.client.renderer.texture.AbstractTexture

object IrisInterop {
    fun shaderPackActive(): Boolean = IrisApi.getInstance().isShaderPackInUse

    fun registerPbr(albedo: AbstractTexture, normal: AbstractTexture, specular: AbstractTexture) {
        val texture = albedo.texture
        if (texture is GlTexture) IrisPbrTextures.register(texture.glId(), normal, specular)
    }

    fun unregisterPbr(albedo: AbstractTexture) {
        val texture = albedo.texture
        if (texture is GlTexture) IrisPbrTextures.unregister(texture.glId())
    }
}
