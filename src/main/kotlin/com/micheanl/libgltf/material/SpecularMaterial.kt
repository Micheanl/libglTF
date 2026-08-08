package com.micheanl.libgltf.material

/**
 * libgltf · SpecularMaterial
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class SpecularMaterial(
    val factor: Float,
    val texture: TextureBinding?,
    val colorFactor: FloatArray,
    val colorTexture: TextureBinding?
)
