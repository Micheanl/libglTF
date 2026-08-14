package com.micheanl.libgltf.model

/**
 * libgltf · GltfLight
 *
 * ```
 * private fun parseLights(root: JsonValue): Array<GltfLight> {
 * ```
 *
 * glTF 光源
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
