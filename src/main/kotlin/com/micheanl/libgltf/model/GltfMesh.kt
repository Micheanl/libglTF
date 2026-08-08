package com.micheanl.libgltf.model

/**
 * libgltf · GltfMesh
 *
 * <pre>{@code
 * GltfMesh(
 * JsonFields.string(mesh, "name", "mesh_$meshIndex"),
 * Array(primitives.size()) { primitiveIndex ->
 * parsePrimitive(primitives[primitiveIndex], decoder, lodPolicy, variantCount)
 * },
 * JsonFields.floats(mesh, "weights")
 * )
 * }</pre>
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
