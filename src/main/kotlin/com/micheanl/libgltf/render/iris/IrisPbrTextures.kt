package com.micheanl.libgltf.render.iris

import net.irisshaders.iris.pbr.texture.PBRTextureHolder
import net.minecraft.client.renderer.texture.AbstractTexture
import java.util.concurrent.ConcurrentHashMap


/**
 * libgltf · IrisPbrTextures
 *
 * ```
 * PBRTextureHolder holder = IrisPbrTextures.get(id);
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object IrisPbrTextures {
    private val holders = ConcurrentHashMap<Int, PBRTextureHolder>()

    @JvmStatic
    fun register(id: Int, normal: AbstractTexture, specular: AbstractTexture) {
        holders[id] = IrisPbrHolder(normal, specular)
    }

    @JvmStatic
    fun unregister(id: Int) {
        holders.remove(id)
    }

    @JvmStatic
    fun get(id: Int): PBRTextureHolder? = holders[id]
}
