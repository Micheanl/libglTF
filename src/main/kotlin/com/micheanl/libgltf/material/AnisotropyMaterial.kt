package com.micheanl.libgltf.material




/**
 * libgltf · AnisotropyMaterial
 *
 * ```
 * AnisotropyMaterial(
 * JsonFields.float(it, "anisotropyStrength"),
 * JsonFields.float(it, "anisotropyRotation"),
 * parseBinding(JsonFields.value(it, "anisotropyTexture"))
 * )
 * ```
 *
 * 各向异性材质扩展
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
