package com.micheanl.libgltf.api




/**
 * libgltf · GltfRenderMode
 *
 * ```
 * var renderMode: GltfRenderMode = GltfRenderMode.AUTO
 * ```
 *
 * 渲染模式：CPU、GPU 或自动
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

enum class GltfRenderMode {
    AUTO,
    GPU_PREFERRED,
    CPU
}
