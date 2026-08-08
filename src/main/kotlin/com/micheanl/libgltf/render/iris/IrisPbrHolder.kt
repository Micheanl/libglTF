package com.micheanl.libgltf.render.iris

import net.irisshaders.iris.pbr.texture.PBRTextureHolder
import net.minecraft.client.renderer.texture.AbstractTexture


/**
 * libgltf · IrisPbrHolder
 *
 * <pre><code>
 * holders[id] = IrisPbrHolder(normal, specular)
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class IrisPbrHolder(
    private val normal: AbstractTexture,
    private val specular: AbstractTexture
) : PBRTextureHolder {
    override fun normalTexture(): AbstractTexture = normal

    override fun specularTexture(): AbstractTexture = specular
}
