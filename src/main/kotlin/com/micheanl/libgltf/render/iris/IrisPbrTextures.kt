package com.micheanl.libgltf.render.iris

/**
 * libgltf · IrisPbrTextures
 *
 * <pre><code>
 * PBRTextureHolder holder = IrisPbrTextures.get(id);
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import net.irisshaders.iris.pbr.texture.PBRTextureHolder
import net.minecraft.client.renderer.texture.AbstractTexture
import java.util.concurrent.ConcurrentHashMap

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
