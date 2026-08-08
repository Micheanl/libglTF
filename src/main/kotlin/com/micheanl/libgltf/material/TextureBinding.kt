package com.micheanl.libgltf.material

import kotlin.math.cos
import kotlin.math.sin




/**
 * libgltf · TextureBinding
 *
 * ```
 * private fun parseBinding(value: JsonValue?): TextureBinding? {
 * ```
 *
 * 纹理绑定：贴图索引与 UV 通道
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

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
