package com.micheanl.libgltf.asset

/**
 * libgltf · MaterialUvTarget
 *
 * <pre><code>
 * return MaterialUvTarget(materialIndex, textureSlot, textureProperty)
 * </code></pre>
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
