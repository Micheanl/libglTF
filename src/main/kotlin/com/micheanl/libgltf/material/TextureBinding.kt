package com.micheanl.libgltf.material

/**
 * libgltf · TextureBinding
 *
 * <pre>{@code
 * return TextureBinding(
 * JsonFields.int(value, "index"),
 * JsonFields.int(transform, "texCoord", JsonFields.int(value, "texCoord", 0)),
 * offset.getOrElse(0) { 0.0f },
 * offset.getOrElse(1) { 0.0f },
 * scale.getOrElse(0) { 1.0f },
 * scale.getOrElse(1) { 1.0f },
 * JsonFields.float(transform, "rotation")
 * )
 * }</pre>
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
