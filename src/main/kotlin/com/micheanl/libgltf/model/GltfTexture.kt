package com.micheanl.libgltf.model

import com.micheanl.libgltf.material.TextureSampler


/**
 * libgltf · GltfTexture
 *
 * <pre><code>
 * GltfTexture(JsonFields.int(texture, "source"), sampler)
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfTexture(
    val imageIndex: Int,
    val sampler: TextureSampler
)
