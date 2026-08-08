package com.micheanl.libgltf.model

/**
 * libgltf · GltfCamera
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfCamera(
    val name: String,
    val type: CameraType,
    val yFov: Float,
    val zNear: Float,
    val zFar: Float,
    val aspectRatio: Float,
    val xMag: Float,
    val yMag: Float
)
