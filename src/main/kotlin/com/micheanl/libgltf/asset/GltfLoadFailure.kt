package com.micheanl.libgltf.asset

/**
 * libgltf · GltfLoadFailure
 *
 * <pre><code>
 * GltfLoadFailure(throwable.message ?: throwable.javaClass.simpleName, throwable)
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


data class GltfLoadFailure(val message: String, val cause: Throwable? = null) : GltfLoadResult
