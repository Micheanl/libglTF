package com.micheanl.libgltf.model

/**
 * libgltf · CameraType
 *
 * ```
 * private fun cameraType(value: String): CameraType = when (value) {
 * ```
 *
 * 相机类型：透视或正交
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

enum class CameraType {
    PERSPECTIVE,
    ORTHOGRAPHIC
}
