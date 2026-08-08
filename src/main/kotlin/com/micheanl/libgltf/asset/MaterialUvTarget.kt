package com.micheanl.libgltf.asset

/**
 * libgltf · MaterialUvTarget
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
