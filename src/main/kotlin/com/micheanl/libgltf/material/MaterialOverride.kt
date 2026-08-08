package com.micheanl.libgltf.material

/**
 * libgltf · MaterialOverride
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import net.minecraft.resources.Identifier

data class MaterialOverride(
    val baseColorFactor: FloatArray? = null,
    val baseColorTextureIndex: Int = -1,
    val baseColorIdentifier: Identifier? = null,
    val alphaCutoff: Float = Float.NaN
)
