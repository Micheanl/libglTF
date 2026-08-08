package com.micheanl.libgltf.asset

import com.micheanl.libgltf.model.GltfAsset


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

data class GltfLoadSuccess(val asset: GltfAsset) : GltfLoadResult
