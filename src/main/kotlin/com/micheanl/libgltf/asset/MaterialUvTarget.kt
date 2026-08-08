package com.micheanl.libgltf.asset

/**
 * libgltf · MaterialUvTarget
 *
 * ```
 * private fun materialUvTarget(target: JsonValue): MaterialUvTarget? {
 * ```
 *
 * 材质 UV 目标通道
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class MaterialUvTarget(
    val materialIndex: Int,
    val textureSlot: Int,
    val textureProperty: Int
)
