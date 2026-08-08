package com.micheanl.libgltf.render.gl

import com.micheanl.libgltf.render.gpu.GltfGpuBackend
import com.micheanl.libgltf.render.gpu.GltfGpuDriver
import com.micheanl.libgltf.render.vulkan.GltfGpuDrivenSettings
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
                return null
            }
            val cache = GltfGlMeshPipelineCache()
            return GltfGlGpuDriven(cache.takeIf { it.supported })
        }
    }
}
