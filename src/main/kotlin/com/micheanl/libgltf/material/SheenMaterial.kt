package com.micheanl.libgltf.material

/**
 * libgltf · SheenMaterial
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class SheenMaterial(
    val colorFactor: FloatArray,
    val colorTexture: TextureBinding?,
    val roughnessFactor: Float,
    val roughnessTexture: TextureBinding?
)
