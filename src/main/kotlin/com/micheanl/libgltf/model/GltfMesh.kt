package com.micheanl.libgltf.model

/**
 * libgltf · GltfMesh
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfMesh(
    val name: String,
    val primitives: Array<GltfPrimitive>,
    val weights: FloatArray
)
