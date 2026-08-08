package com.micheanl.libgltf.render

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
