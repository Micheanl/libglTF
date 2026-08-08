package com.micheanl.libgltf.render




/**
 * libgltf · GpuCapabilities
 *
 * ```
 * fun gpuCapabilities(): GpuCapabilities
 * ```
 *
 * GPU 能力集合
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GpuCapabilities(
    val backend: GpuBackendType,
    val instancing: Boolean,
    val shaderDrawParameters: Boolean,
    val multiDrawDirectInterleaved: Boolean,
    val multiDrawDirectSeparate: Boolean,
    val drawIndirect: Boolean,
    val multiDrawIndirect: Boolean,
    val nonZeroFirstInstance: Boolean,
    val persistentMapping: Boolean,
    val meshShaderExtensionPresent: Boolean,
    val nativeMeshShaderActive: Boolean,
    val meshShaderNvActive: Boolean,
    val path: GpuPath
)
