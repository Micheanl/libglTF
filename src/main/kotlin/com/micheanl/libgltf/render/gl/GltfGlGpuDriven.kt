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
            if (!caps.GL_EXT_mesh_shader || !profile.preferMeshShader || !GltfGpuDrivenSettings.meshShader) {
                return null
            }
            return GltfGlGpuDriven(GltfGlMeshPipelineCache().takeIf { it.supported })
        }
    }
}
