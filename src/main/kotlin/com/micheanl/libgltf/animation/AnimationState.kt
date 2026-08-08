package com.micheanl.libgltf.animation




/**
 * libgltf · AnimationState
 *
 * ```
 * val states: Array<AnimationState>,
 * ```
 *
 * 状态机中的单个状态
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class AnimationState(
    val name: String,
    val segment: AnimationSegment,
    val looping: Boolean = true,
    val speed: Float = 1.0f
)
