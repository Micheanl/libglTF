package com.micheanl.libgltf.material

/**
 * libgltf · IridescenceMaterial
 *
 * <pre><code>
 * IridescenceMaterial(
 * JsonFields.float(it, "iridescenceFactor"),
 * parseBinding(JsonFields.value(it, "iridescenceTexture")),
 * JsonFields.float(it, "iridescenceIor", 1.3f),
 * JsonFields.float(it, "iridescenceThicknessMinimum", 100.0f),
 * JsonFields.float(it, "iridescenceThicknessMaximum", 400.0f),
 * parseBinding(JsonFields.value(it, "iridescenceThicknessTexture"))
 * )
 * </code></pre>
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
