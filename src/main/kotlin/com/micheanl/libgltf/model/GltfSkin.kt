package com.micheanl.libgltf.model

/**
 * libgltf · GltfSkin
 *
 * ```
 * GltfSkin(JsonFields.string(skin, "name", "skin_$index"), joints, matrices)
 * ```
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
