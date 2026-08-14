package com.micheanl.libgltf.render.iris

import com.mojang.renderpearl.backend.opengl.GlTexture
import net.irisshaders.iris.api.v0.IrisApi
import net.minecraft.client.renderer.texture.AbstractTexture

/**
 * libgltf · IrisInterop
 *
 * ```
 * fun shaderPackActive(): Boolean = loaded && IrisInterop.shaderPackActive()
 * ```
 *
 * Iris 互操作
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

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
