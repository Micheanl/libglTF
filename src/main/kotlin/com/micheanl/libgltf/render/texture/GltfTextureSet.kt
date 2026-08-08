package com.micheanl.libgltf.render.texture

import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier

/**
 * libgltf · GltfTextureSet
 *
 * ```
 * textures: GltfTextureSet,
 * ```
 *
 * 资源纹理集合
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GltfTextureSet(
    val identifiers: Array<Identifier>,
    private val textures: Array<GltfDynamicTexture>,
    private val materialIdentifiers: Array<Identifier>,
    private val materialTextures: Array<GltfMaterialTexture?>,
    val fallback: Identifier
) : AutoCloseable {
    fun identifier(index: Int): Identifier = identifiers.getOrElse(index) { fallback }

    fun materialIdentifier(index: Int): Identifier = materialIdentifiers.getOrElse(index) { fallback }

    override fun close() {
        val manager = Minecraft.getInstance().textureManager
        for (index in materialTextures.indices) {
            val texture = materialTextures[index] ?: continue
            texture.close()
            manager.release(materialIdentifiers[index])
        }
        for (identifier in identifiers) manager.release(identifier)
        manager.release(fallback)
    }
}
