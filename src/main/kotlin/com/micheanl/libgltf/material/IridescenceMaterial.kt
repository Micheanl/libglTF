package com.micheanl.libgltf.material

/**
 * libgltf · IridescenceMaterial
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class IridescenceMaterial(
    val factor: Float,
    val texture: TextureBinding?,
    val ior: Float,
    val thicknessMinimum: Float,
    val thicknessMaximum: Float,
    val thicknessTexture: TextureBinding?
)
