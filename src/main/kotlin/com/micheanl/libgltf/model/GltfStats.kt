package com.micheanl.libgltf.model

/**
 * libgltf · GltfStats
 *
 * ```
 * val stats = GltfStats(
 * resolvedNodes.size,
 * meshes.size,
 * primitiveCount,
 * triangleCount,
 * materials.size,
 * textures.size,
 * skins.size,
 * morphTargetCount
 * )
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfStats(
    val nodeCount: Int,
    val meshCount: Int,
    val primitiveCount: Int,
    val triangleCount: Long,
    val materialCount: Int,
    val textureCount: Int,
    val skinCount: Int,
    val morphTargetCount: Int
)
