package com.micheanl.libgltf.model

/**
 * libgltf · GltfPrimitive
 *
 * ```
 * return GltfPrimitive(
 * vertices,
 * skin,
 * lodIndices,
 * vertexCount,
 * JsonFields.int(value, "material", 0),
 * materialMappings,
 * mode,
 * bounds,
 * morphPositions,
 * morphNormals,
 * targetCount
 * )
 * ```
 *
 * 网格基元与 LOD 索引
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


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
