package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationComparison
 *
 * ```
 * val comparison: AnimationComparison,
 * ```
 *
 * 状态机比较条件
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

enum class AnimationComparison {
    EQUAL,
    NOT_EQUAL,
    GREATER,
    GREATER_OR_EQUAL,
    LESS,
    LESS_OR_EQUAL
}
