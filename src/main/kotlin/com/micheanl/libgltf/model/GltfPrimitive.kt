package com.micheanl.libgltf.model

import java.nio.ByteBuffer

/**
 * libgltf · GltfPrimitive
 *
 * ```
 * ): GltfPrimitive {
 * ```
 *
 * 网格基元与 LOD 索引
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

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
