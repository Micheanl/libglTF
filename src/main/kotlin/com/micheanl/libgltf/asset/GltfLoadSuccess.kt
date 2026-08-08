package com.micheanl.libgltf.asset

import com.micheanl.libgltf.model.GltfAsset


/**
 * libgltf · GltfLoadSuccess
 *
 * ```
 * GltfLoadSuccess(parse(path, lodPolicy))
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfLoadSuccess(val asset: GltfAsset) : GltfLoadResult
