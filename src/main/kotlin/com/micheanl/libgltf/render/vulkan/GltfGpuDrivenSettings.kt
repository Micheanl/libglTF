package com.micheanl.libgltf.render.vulkan

object GltfGpuDrivenSettings {
    val enabled: Boolean = booleanProperty("libgltf.vulkan.gpuDriven", true)
    val meshShader: Boolean = booleanProperty("libgltf.vulkan.meshShader", true)
    @Volatile
    var instanceCulling: Boolean = booleanProperty("libgltf.vulkan.instanceCulling", true)
    @Volatile
    var meshletCulling: Boolean = booleanProperty("libgltf.vulkan.meshletCulling", true)
    val benchmark: Boolean = booleanProperty("libgltf.vulkan.benchmark", false)
    val force: Boolean = booleanProperty("libgltf.vulkan.gpuDriven.force", false)

    @Volatile
    var meshShaderOverride: Boolean? = null

    @Volatile
    var debugMeshMinimal: Boolean = booleanProperty("libgltf.vulkan.meshShader.debugMinimal", false)

    @Volatile
    var debugMeshFlat: Boolean = booleanProperty("libgltf.vulkan.meshShader.debugFlat", false)

    @Volatile
    var debugMeshCounters: Boolean = booleanProperty("libgltf.vulkan.meshShader.debugCounters", false)

    @Volatile
    var meshGroupLimit: Int = intProperty("libgltf.vulkan.meshShader.groupLimit", 65535)

    fun meshShaderEnabled(): Boolean = meshShaderOverride ?: meshShader

    fun profitable(instanceCount: Int, meshletCount: Int): Boolean = force ||
        instanceCount >= MIN_INSTANCE_COUNT || instanceCount * meshletCount >= MIN_COMMAND_COUNT

    private fun booleanProperty(name: String, default: Boolean): Boolean =
        System.getProperty(name)?.toBooleanStrictOrNull() ?: default

    private fun intProperty(name: String, default: Int): Int =
        System.getProperty(name)?.toIntOrNull() ?: default

    private const val MIN_INSTANCE_COUNT = 16
    private const val MIN_COMMAND_COUNT = 32
}
