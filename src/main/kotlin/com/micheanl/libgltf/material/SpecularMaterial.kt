package com.micheanl.libgltf.material

/**
 * libgltf · SpecularMaterial
 *
 * ```
 * SpecularMaterial(
 * JsonFields.float(it, "specularFactor", 1.0f),
 * parseBinding(JsonFields.value(it, "specularTexture")),
 * JsonFields.floats(it, "specularColorFactor", floatArrayOf(1.0f, 1.0f, 1.0f)),
 * parseBinding(JsonFields.value(it, "specularColorTexture"))
 * )
 * ```
 *
 * 高光材质扩展
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
