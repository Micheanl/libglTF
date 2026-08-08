package com.micheanl.libgltf.render.gl

import com.micheanl.libgltf.render.gpu.GltfGpuBackend
import com.micheanl.libgltf.render.gpu.GltfGpuDriver
import com.micheanl.libgltf.render.vulkan.GltfGpuDrivenSettings
import com.mojang.logging.LogUtils
import org.lwjgl.opengl.GL

class GltfGlGpuDriven private constructor(
    val meshPipelines: GltfGlMeshPipelineCache?
) : GltfGpuDriver {
    override val meshSupported: Boolean
        get() = meshPipelines?.supported == true

    override fun close() {
        meshPipelines?.close()
    }

    companion object {
        fun create(): GltfGlGpuDriven? {
            val caps = GL.getCapabilities()
            val profile = GltfGpuBackend.vendorProfile()
            val extensionAvailable = caps.GL_EXT_mesh_shader || caps.GL_NV_mesh_shader
            if (!extensionAvailable || !profile.preferMeshShader || !GltfGpuDrivenSettings.meshShaderEnabled()) {
                LOGGER.info(
                    "libgltf GL mesh shader disabled (extension={} vendor={} enabled={})",
                    extensionAvailable,
                    profile.vendor,
                    GltfGpuDrivenSettings.meshShaderEnabled()
                )
                return null
            }
            val cache = GltfGlMeshPipelineCache()
            LOGGER.info(
                "libgltf GL mesh shader enabled vendor={} nv={} ext={} supported={}",
                profile.vendor,
                caps.GL_NV_mesh_shader,
                caps.GL_EXT_mesh_shader,
                cache.supported
            )
            return GltfGlGpuDriven(cache.takeIf { it.supported })
        }

        private val LOGGER = LogUtils.getLogger()
    }
}
