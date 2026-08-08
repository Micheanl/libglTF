package com.micheanl.libgltf.model

/**
 * libgltf · LightType
 *
 * ```
 * private fun lightType(value: String): LightType = when (value) {
 * ```
 *
 * 光源类型：方向、点或聚光
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

enum class LightType {
    POINT,
    SPOT,
    DIRECTIONAL
}
