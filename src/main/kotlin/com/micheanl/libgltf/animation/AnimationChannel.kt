package com.micheanl.libgltf.animation




/**
 * libgltf · AnimationChannel
 *
 * ```
 * val channels: Array<AnimationChannel>
 * ```
 *
 * 动画通道：属性路径与采样曲线
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
