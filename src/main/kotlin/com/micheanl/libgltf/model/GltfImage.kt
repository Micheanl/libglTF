package com.micheanl.libgltf.model

/**
 * libgltf · GltfImage
 *
 * ```
 * GltfImage(JsonFields.string(image, "name", "image_$index"), mime, bytes)
 * ```
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
