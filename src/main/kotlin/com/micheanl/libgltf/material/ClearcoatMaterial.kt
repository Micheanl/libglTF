package com.micheanl.libgltf.material

/**
 * libgltf · ClearcoatMaterial
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class ClearcoatMaterial(
    val factor: Float,
    val texture: TextureBinding?,
    val roughnessFactor: Float,
    val roughnessTexture: TextureBinding?,
    val normalTexture: TextureBinding?,
    val normalScale: Float
)
