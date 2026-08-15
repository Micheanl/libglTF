package com.micheanl.libgltf.render

import com.micheanl.libgltf.model.GltfPrimitive
import com.micheanl.libgltf.model.PrimitiveMode
import com.micheanl.renderapi.MeshSource
import java.nio.ByteBuffer

class GltfMeshSource(private val primitive: GltfPrimitive) : MeshSource {
    override val vertices: ByteBuffer = primitive.vertices
    override val skin: ByteBuffer? = primitive.skin
    override val lodIndices: Array<IntArray> = primitive.lodIndices
    override val vertexCount: Int = primitive.vertexCount
    override val bounds: FloatArray = primitive.bounds
    override val triangles: Boolean = primitive.mode == PrimitiveMode.TRIANGLES
}
