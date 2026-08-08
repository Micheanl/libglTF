package com.micheanl.libgltf.render.vulkan

import java.util.Properties

object GltfGpuDrivenSettings {
    val enabled: Boolean = booleanProperty("libgltf.vulkan.gpuDriven", true)
    val meshShader: Boolean = booleanProperty("libgltf.vulkan.meshShader", true)
    @Volatile
    var instanceCulling: Boolean = booleanProperty("libgltf.vulkan.instanceCulling", true)
    @Volatile
    var meshletCulling: Boolean = booleanProperty("libgltf.vulkan.meshletCulling", true)

    @Volatile
    var occlusionCulling: Boolean = booleanProperty("libgltf.vulkan.meshShader.occlusionCulling", false)
    @Volatile
    var benchmark: Boolean = booleanProperty("libgltf.vulkan.benchmark", false)
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

    @Volatile
    var meshBatchSize: Int = intProperty("libgltf.vulkan.meshShader.batchSize", 4)

    fun meshShaderEnabled(): Boolean = meshShaderOverride ?: meshShader

    fun applyConfig(properties: Properties): List<String> {
        val applied = ArrayList<String>()
        properties.getProperty("meshShader")?.lowercase()?.let {
            when (it) {
                "on", "true" -> {
                    meshShaderOverride = true
                    applied += "meshShader=on"
                }
                "off", "false" -> {
                    meshShaderOverride = false
                    applied += "meshShader=off"
                }
                "auto" -> {
                    meshShaderOverride = null
                    applied += "meshShader=auto"
                }
            }
        }
        properties.getProperty("meshBatchSize")?.toIntOrNull()?.let {
            meshBatchSize = it.coerceIn(1, 4)
            applied += "meshBatchSize=$meshBatchSize"
        }
        properties.getProperty("occlusionCulling")?.toBooleanStrictOrNull()?.let {
            occlusionCulling = it
            applied += "occlusionCulling=$it"
        }
        properties.getProperty("instanceCulling")?.toBooleanStrictOrNull()?.let {
            instanceCulling = it
            applied += "instanceCulling=$it"
        }
        properties.getProperty("meshletCulling")?.toBooleanStrictOrNull()?.let {
            meshletCulling = it
            applied += "meshletCulling=$it"
        }
        properties.getProperty("groupLimit")?.toIntOrNull()?.let {
            meshGroupLimit = it.coerceAtLeast(0)
            applied += "groupLimit=$meshGroupLimit"
        }
        properties.getProperty("counters")?.toBooleanStrictOrNull()?.let {
            debugMeshCounters = it
            applied += "counters=$it"
        }
        properties.getProperty("benchmark")?.toBooleanStrictOrNull()?.let {
            benchmark = it
            applied += "benchmark=$it"
        }
        properties.getProperty("minimal")?.toBooleanStrictOrNull()?.let {
            debugMeshMinimal = it
            applied += "minimal=$it"
        }
        properties.getProperty("flat")?.toBooleanStrictOrNull()?.let {
            debugMeshFlat = it
            applied += "flat=$it"
        }
        return applied
    }

    fun profitable(instanceCount: Int, meshletCount: Int): Boolean = force ||
        instanceCount >= MIN_INSTANCE_COUNT || instanceCount * meshletCount >= MIN_COMMAND_COUNT

    private fun booleanProperty(name: String, default: Boolean): Boolean =
        System.getProperty(name)?.toBooleanStrictOrNull() ?: default

    private fun intProperty(name: String, default: Int): Int =
        System.getProperty(name)?.toIntOrNull() ?: default

    private const val MIN_INSTANCE_COUNT = 16
    private const val MIN_COMMAND_COUNT = 32
}
