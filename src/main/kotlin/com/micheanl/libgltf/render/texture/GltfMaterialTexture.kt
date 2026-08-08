package com.micheanl.libgltf.render.texture

import com.micheanl.libgltf.render.iris.IrisCompat
import net.minecraft.resources.Identifier




/**
 * libgltf · GltfMaterialTexture
 *
 * ```
 * val materialTextures = arrayOfNulls<GltfMaterialTexture>(asset.materials.size)
 * ```
 *
 * 材质贴图的 GPU 纹理
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GltfMaterialTexture(
    val identifier: Identifier,
    private val albedo: GltfDynamicTexture,
    private val normal: GltfDynamicTexture,
    private val specular: GltfDynamicTexture
) : AutoCloseable {
    private var closed: Boolean = false

    init {
        IrisCompat.registerPbr(albedo, normal, specular)
    }

    override fun close() {
        if (closed) return
        closed = true
        IrisCompat.unregisterPbr(albedo)
        normal.close()
        specular.close()
    }
}
