package com.micheanl.libgltf.model

/**
 * libgltf · GltfImage
 *
 * ```
 * private fun parseImages(root: JsonValue, resolver: GltfBufferResolver): Array<GltfImage> {
 * ```
 *
 * glTF 图像资源
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
