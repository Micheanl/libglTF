package com.micheanl.libgltf.model

import java.nio.ByteBuffer

data class GltfPrimitive(
    val vertices: ByteBuffer,
    val skin: ByteBuffer?,
    val lodIndices: Array<IntArray>,
    val vertexCount: Int,
    val materialIndex: Int,
    val materialMappings: IntArray,
    val mode: PrimitiveMode,
    val bounds: FloatArray,
    val morphPositions: FloatArray,
    val morphNormals: FloatArray,
    val morphTargetCount: Int
)
