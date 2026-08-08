package com.micheanl.libgltf.material

/**
 * libgltf · TextureSampler
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class TextureSampler(
    val magnification: TextureFilter,
    val minification: TextureFilter,
    val mipmap: TextureFilter?,
    val wrapS: TextureWrap,
    val wrapT: TextureWrap
) {
    companion object {
        @JvmField
        val DEFAULT: TextureSampler = TextureSampler(
            TextureFilter.LINEAR,
            TextureFilter.LINEAR,
            TextureFilter.LINEAR,
            TextureWrap.REPEAT,
            TextureWrap.REPEAT
        )
    }
}
