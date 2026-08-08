package com.micheanl.libgltf.asset

/**
 * libgltf · GltfLoadFailure
 *
 * ```
 * GltfLoadFailure(throwable.message ?: throwable.javaClass.simpleName, throwable)
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfLoadFailure(val message: String, val cause: Throwable? = null) : GltfLoadResult
