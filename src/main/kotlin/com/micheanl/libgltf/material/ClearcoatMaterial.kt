package com.micheanl.libgltf.material




/**
 * libgltf · ClearcoatMaterial
 *
 * ```
 * ClearcoatMaterial(
 * JsonFields.float(it, "clearcoatFactor"),
 * parseBinding(JsonFields.value(it, "clearcoatTexture")),
 * JsonFields.float(it, "clearcoatRoughnessFactor"),
 * parseBinding(JsonFields.value(it, "clearcoatRoughnessTexture")),
 * parseBinding(JsonFields.value(it, "clearcoatNormalTexture")),
 * JsonFields.float(JsonFields.value(it, "clearcoatNormalTexture"), "scale", 1.0f)
 * )
 * ```
 *
 * 清漆材质扩展
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
