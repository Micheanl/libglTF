package com.micheanl.libgltf.render

/**
 * libgltf · GpuPath
 *
 * ```
 * GpuPath.CPU
 * ```
 *
 * 渲染路径：CPU 或实例化 GPU
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

enum class GpuPath {
    CPU,
    INSTANCED,
    MESH_SHADER
}
