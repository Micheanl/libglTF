package com.micheanl.libgltf.model

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
