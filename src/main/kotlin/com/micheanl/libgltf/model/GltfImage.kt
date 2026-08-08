package com.micheanl.libgltf.model

/**
 * libgltf · GltfImage
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfImage(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray
)
