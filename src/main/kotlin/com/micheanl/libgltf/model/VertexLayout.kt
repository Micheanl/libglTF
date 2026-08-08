package com.micheanl.libgltf.model

/**
 * libgltf · VertexLayout
 *
 * ```
 * val vertices = ByteBuffer.allocate(vertexCount * VertexLayout.STRIDE).order(ByteOrder.nativeOrder())
 * ```
 *
 * 顶点布局常量：属性偏移与步长
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object VertexLayout {
    const val STRIDE: Int = 60
    const val POSITION: Int = 0
    const val NORMAL: Int = 12
    const val TANGENT: Int = 24
    const val UV0: Int = 40
    const val UV1: Int = 48
    const val COLOR: Int = 56
    const val SKIN_STRIDE: Int = 16
    const val JOINTS: Int = 0
    const val WEIGHTS: Int = 8
}
