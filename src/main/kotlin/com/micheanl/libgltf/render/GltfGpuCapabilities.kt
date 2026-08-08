package com.micheanl.libgltf.render

data class GltfGpuCapabilities(
    val backend: GltfGpuBackendType,
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
    val path: GltfGpuPath
)
