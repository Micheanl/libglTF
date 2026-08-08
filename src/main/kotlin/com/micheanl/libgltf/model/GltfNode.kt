package com.micheanl.libgltf.model




/**
 * libgltf · GltfNode
 *
 * ```
 * private fun parseNodes(root: JsonValue, decoder: AccessorDecoder): Array<GltfNode> {
 * ```
 *
 * glTF 节点：变换与网格引用
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfNode(
    val name: String,
    val parentIndex: Int,
    val children: IntArray,
    val meshIndex: Int,
    val skinIndex: Int,
    val cameraIndex: Int,
    val lightIndex: Int,
    val instanceMatrices: FloatArray,
    val translation: FloatArray,
    val rotation: FloatArray,
    val scale: FloatArray,
    val matrix: FloatArray?,
    val weights: FloatArray
)
