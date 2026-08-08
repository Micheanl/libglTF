package com.micheanl.libgltf.material

/**
 * libgltf · TextureSampler
 *
 * ```
 * return TextureSampler(
 * mag,
 * min,
 * mipmap,
 * wrap(JsonFields.int(value, "wrapS", 10497)),
 * wrap(JsonFields.int(value, "wrapT", 10497))
 * )
 * ```
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
