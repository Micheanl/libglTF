package com.micheanl.libgltf.model

/**
 * libgltf · GltfTexture
 *
 * <pre>{@code
 * GltfTexture(JsonFields.int(texture, "source"), sampler)
 * }</pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.material.TextureSampler

data class GltfTexture(
    val imageIndex: Int,
    val sampler: TextureSampler
)
