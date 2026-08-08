package com.micheanl.libgltf.material

/**
 * libgltf · TextureSampler
 *
 * ```
 * val sampler = if (samplerIndex >= 0 && samplers != null) parseSampler(samplers[samplerIndex]) else TextureSampler.DEFAULT
 * ```
 *
 * glTF 采样器配置
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
