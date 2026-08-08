package com.micheanl.libgltf.material

/**
 * libgltf · SheenMaterial
 *
 * <pre>{@code
 * SheenMaterial(
 * JsonFields.floats(it, "sheenColorFactor", floatArrayOf(0.0f, 0.0f, 0.0f)),
 * parseBinding(JsonFields.value(it, "sheenColorTexture")),
 * JsonFields.float(it, "sheenRoughnessFactor"),
 * parseBinding(JsonFields.value(it, "sheenRoughnessTexture"))
 * )
 * }</pre>
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
