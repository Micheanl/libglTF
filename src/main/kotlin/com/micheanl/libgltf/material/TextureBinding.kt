package com.micheanl.libgltf.material

/**
 * libgltf · TextureBinding
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import kotlin.math.cos
import kotlin.math.sin

data class TextureBinding(
    val textureIndex: Int,
    val texCoord: Int,
    val offsetX: Float,
    val offsetY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float
) {
    val cosine: Float = cos(rotation)
    val sine: Float = sin(rotation)
}
