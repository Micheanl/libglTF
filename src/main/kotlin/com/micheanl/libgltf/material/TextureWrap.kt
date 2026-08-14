package com.micheanl.libgltf.material

/**
 * libgltf · TextureWrap
 *
 * ```
 * private fun wrap(value: Int): TextureWrap = when (value) {
 * ```
 *
 * 纹理包裹模式
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

enum class TextureWrap {
    CLAMP_TO_EDGE,
    MIRRORED_REPEAT,
    REPEAT
}
