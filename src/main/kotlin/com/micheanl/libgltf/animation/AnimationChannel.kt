package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationChannel
 *
 * <pre><code>
 * parsed += AnimationChannel(
 * -1,
 * AnimationPath.MATERIAL_UV,
 * interpolation,
 * input,
 * output,
 * components,
 * materialTarget.materialIndex,
 * materialTarget.textureSlot,
 * materialTarget.textureProperty
 * )
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


data class AnimationChannel(
    val nodeIndex: Int,
    val path: AnimationPath,
    val interpolation: Interpolation,
    val times: FloatArray,
    val values: FloatArray,
    val componentCount: Int,
    val materialIndex: Int = -1,
    val textureSlot: Int = 0,
    val textureProperty: Int = 0
)
