package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationParameter
 *
 * ```
 * val parameter: AnimationParameter,
 * ```
 *
 * 动画参数
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class AnimationParameter(
    val name: String,
    val index: Int,
    val type: AnimationParameterType
)
