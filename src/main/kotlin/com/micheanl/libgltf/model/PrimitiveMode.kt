package com.micheanl.libgltf.model

/**
 * libgltf · PrimitiveMode
 *
 * ```
 * if (primitive.mode == PrimitiveMode.TRIANGLES) primitive.lodIndices[0].size.toLong() / 3L else 0L
 * ```
 *
 * 基元模式：三角形、线或点
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

enum class PrimitiveMode {
    POINTS,
    LINES,
    TRIANGLES
}
