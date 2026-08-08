package com.micheanl.libgltf.asset

/**
 * libgltf · GltfLoadSuccess
 *
 * <pre><code>
 * GltfLoadSuccess(parse(path, lodPolicy))
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.model.GltfAsset

data class GltfLoadSuccess(val asset: GltfAsset) : GltfLoadResult
