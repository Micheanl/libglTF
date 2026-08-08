package com.micheanl.libgltf.render

/**
 * libgltf · GltfConfig
 *
 * ```
 * GltfConfig.load()
 * ```
 *
 * config 文件加载与默认生成
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.render.vulkan.RenderConfig
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

object GltfConfig {
    fun load() {
        val file = FabricLoader.getInstance().configDir.resolve("libgltf.properties")
        if (!Files.isRegularFile(file)) {
            writeDefault(file)
            return
        }
        val properties = Properties()
        Files.newInputStream(file).use { properties.load(it) }
        RenderConfig.applyConfig(properties)
    }

    private fun writeDefault(file: Path) {
        val content = """
            # libgltf performance configuration
            # meshShader: auto | on | off (auto = highest performance path for the device)
            meshShader=auto
            # meshBatchSize: 1-4 (NV mesh shader processes this many meshlets per workgroup)
            meshBatchSize=4
            # occlusionCulling: true | false (previous-frame depth occlusion; helps complex scenes)
            occlusionCulling=false
            instanceCulling=true
            meshletCulling=true
            # groupLimit: max mesh workgroups per draw call (0 = unlimited)
            groupLimit=65535
        """.trimIndent()
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

}
