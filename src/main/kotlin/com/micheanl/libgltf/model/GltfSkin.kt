package com.micheanl.libgltf.model




/**
 * libgltf · GltfSkin
 *
 * ```
 * private fun parseSkins(root: JsonValue, decoder: AccessorDecoder): Array<GltfSkin> {
 * ```
 *
 * glTF 蒙皮：关节与逆绑定矩阵
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfSkin(
    val name: String,
    val joints: IntArray,
    val inverseBindMatrices: FloatArray
)
