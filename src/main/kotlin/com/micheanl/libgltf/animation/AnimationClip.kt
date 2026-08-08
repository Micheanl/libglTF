package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationClip
 *
 * ```
 * clip: AnimationClip,
 * ```
 *
 * 动画片段：通道集合与总时长
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class AnimationClip(
    val name: String,
    val durationSeconds: Float,
    val channels: Array<AnimationChannel>
)
