package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationPath
 *
 * ```
 * val path: AnimationPath,
 * ```
 *
 * 动画属性路径：平移、旋转、缩放等
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

enum class AnimationPath {
    TRANSLATION,
    ROTATION,
    SCALE,
    WEIGHTS,
    MATERIAL_UV,
    MATERIAL_FACTOR
}
