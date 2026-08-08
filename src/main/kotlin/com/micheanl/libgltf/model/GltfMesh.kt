package com.micheanl.libgltf.model

/**
 * libgltf · GltfMesh
 *
 * ```
 * GltfMesh(
 * JsonFields.string(mesh, "name", "mesh_$meshIndex"),
 * Array(primitives.size()) { primitiveIndex ->
 * parsePrimitive(primitives[primitiveIndex], decoder, lodPolicy, variantCount)
 * },
 * JsonFields.floats(mesh, "weights")
 * )
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfMesh(
    val name: String,
    val primitives: Array<GltfPrimitive>,
    val weights: FloatArray
)
