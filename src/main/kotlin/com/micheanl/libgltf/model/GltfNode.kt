package com.micheanl.libgltf.model

/**
 * libgltf · GltfNode
 *
 * <pre>{@code
 * GltfNode(
 * JsonFields.string(node, "name", "node_$index"),
 * -1,
 * JsonFields.ints(node, "children"),
 * JsonFields.int(node, "mesh"),
 * JsonFields.int(node, "skin"),
 * JsonFields.int(node, "camera"),
 * JsonFields.int(punctualLight, "light"),
 * instanceMatrices,
 * JsonFields.floats(node, "translation", floatArrayOf(0.0f, 0.0f, 0.0f)),
 * JsonFields.floats(node, "rotation", floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)),
 * JsonFields.floats(node, "scale", floatArrayOf(1.0f, 1.0f, 1.0f)),
 * JsonFields.value(node, "matrix")?.let { JsonFields.floats(node, "matrix") },
 * JsonFields.floats(node, "weights")
 * }</pre>
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
