package com.micheanl.libgltf.model

/**
 * libgltf · GltfLight
 *
 * <pre><code>
 * GltfLight(
 * JsonFields.string(value, "name", "light_$index"),
 * lightType(JsonFields.string(value, "type")),
 * JsonFields.floats(value, "color", floatArrayOf(1.0f, 1.0f, 1.0f)),
 * JsonFields.float(value, "intensity", 1.0f),
 * JsonFields.float(value, "range", -1.0f),
 * JsonFields.float(spot, "innerConeAngle", 0.0f),
 * JsonFields.float(spot, "outerConeAngle", 0.7853982f)
 * )
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfLight(
    val name: String,
    val type: LightType,
    val color: FloatArray,
    val intensity: Float,
    val range: Float,
    val spotInnerConeAngle: Float,
    val spotOuterConeAngle: Float
)
