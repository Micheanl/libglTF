package com.micheanl.libgltf.render.iris

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.renderer.texture.AbstractTexture


/**
 * libgltf · IrisCompat
 *
 * ```
 * IrisCompat.registerPbr(albedo, normal, specular)
 * ```
 *
 * Iris 兼容性检测
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object IrisCompat {
    private val loaded: Boolean = FabricLoader.getInstance().isModLoaded("iris")

    fun available(): Boolean = loaded

    fun shaderPackActive(): Boolean = loaded && IrisInterop.shaderPackActive()

    fun registerPbr(albedo: AbstractTexture, normal: AbstractTexture, specular: AbstractTexture) {
        if (loaded) IrisInterop.registerPbr(albedo, normal, specular)
    }

    fun unregisterPbr(albedo: AbstractTexture) {
        if (loaded) IrisInterop.unregisterPbr(albedo)
    }
}
