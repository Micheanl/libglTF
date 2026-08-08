package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationClip
 *
 * <pre><code>
 * AnimationClip(
 * JsonFields.string(animation, "name", "animation_$animationIndex"),
 * duration,
 * parsed.toTypedArray()
 * )
 * </code></pre>
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
