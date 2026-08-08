package com.micheanl.libgltf.model

data class GltfNode(
    val name: String,
    val parentIndex: Int,
    val children: IntArray,
    val meshIndex: Int,
    val skinIndex: Int,
    val cameraIndex: Int,
    val instanceMatrices: FloatArray,
    val translation: FloatArray,
    val rotation: FloatArray,
    val scale: FloatArray,
    val matrix: FloatArray?,
    val weights: FloatArray
)
