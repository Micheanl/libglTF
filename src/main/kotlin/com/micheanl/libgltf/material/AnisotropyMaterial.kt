package com.micheanl.libgltf.material

/**
 * libgltf · AnisotropyMaterial
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class AnisotropyMaterial(
    val strength: Float,
    val rotation: Float,
    val texture: TextureBinding?
)
