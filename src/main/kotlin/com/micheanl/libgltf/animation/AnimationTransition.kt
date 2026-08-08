package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationTransition
 *
 * ```
 * AnimationTransition(
 * fromState,
 * toState,
 * conditions.copyOf(),
 * fadeSeconds,
 * minimumStateSeconds,
 * exitTime,
 * priority
 * )
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class AnimationTransition(
    val fromState: Int,
    val toState: Int,
    val conditions: Array<out AnimationCondition> = emptyArray(),
    val fadeSeconds: Float = 0.0f,
    val minimumStateSeconds: Float = 0.0f,
    val exitTime: Float = -1.0f,
    val priority: Int = 0
) {
    init {
        require(fromState >= -1)
        require(toState >= 0)
        require(fadeSeconds >= 0.0f)
        require(minimumStateSeconds >= 0.0f)
        require(exitTime <= 1.0f)
    }
}
