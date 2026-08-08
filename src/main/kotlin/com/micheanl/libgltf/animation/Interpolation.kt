package com.micheanl.libgltf.animation




/**
 * libgltf · Interpolation
 *
 * ```
 * val interpolation: Interpolation,
 * ```
 *
 * 动画插值：线性、步进或三次样条
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

enum class Interpolation {
    LINEAR,
    STEP,
    CUBIC_SPLINE
}
