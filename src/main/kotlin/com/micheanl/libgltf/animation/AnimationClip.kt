package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationClip
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
