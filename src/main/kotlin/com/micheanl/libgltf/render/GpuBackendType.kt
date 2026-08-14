package com.micheanl.libgltf.render

/**
 * libgltf · GpuBackendType
 *
 * ```
 * val gl = GpuBackend.capabilities().backend == GpuBackendType.OPENGL
 * ```
 *
 * GPU 后端类型：Vulkan、OpenGL 或未知
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

enum class GpuBackendType {
    OPENGL,
    VULKAN,
    UNKNOWN
}
