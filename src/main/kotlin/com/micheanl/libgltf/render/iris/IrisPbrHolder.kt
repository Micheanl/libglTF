package com.micheanl.libgltf.render.iris

/**
 * libgltf · IrisPbrHolder
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import net.irisshaders.iris.pbr.texture.PBRTextureHolder
import net.minecraft.client.renderer.texture.AbstractTexture

class IrisPbrHolder(
    private val normal: AbstractTexture,
    private val specular: AbstractTexture
) : PBRTextureHolder {
    override fun normalTexture(): AbstractTexture = normal

    override fun specularTexture(): AbstractTexture = specular
}
