package com.micheanl.libgltf.model

import com.micheanl.libgltf.material.TextureSampler




/**
 * libgltf · GltfTexture
 *
 * ```
 * private fun parseTextures(root: JsonValue): Array<GltfTexture> {
 * ```
 *
 * glTF 纹理：图像与采样器组合
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfTexture(
    val imageIndex: Int,
    val sampler: TextureSampler
)
