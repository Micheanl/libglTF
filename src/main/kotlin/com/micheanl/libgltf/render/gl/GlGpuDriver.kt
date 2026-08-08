package com.micheanl.libgltf.render.gl

import com.micheanl.libgltf.render.gpu.GpuBackend
import com.micheanl.libgltf.render.gpu.GpuDriver
import com.micheanl.libgltf.render.vulkan.RenderConfig
import org.lwjgl.opengl.GL




/**
 * libgltf · GlGpuDriver
 *
 * ```
 * (driver is VulkanGpuDriver || driver is GlGpuDriver)
 * ```
 *
 * OpenGL 后端的 GPU 驱动
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GlGpuDriver private constructor(
    val meshPipelines: GlMeshPipelineCache?
) : GpuDriver {
    override val meshSupported: Boolean
        get() = meshPipelines?.supported == true

    override fun close() {
        meshPipelines?.close()
    }

    companion object {
        fun create(): GlGpuDriver? {
            val caps = GL.getCapabilities()
            val profile = GpuBackend.vendorProfile()
            val extensionAvailable = caps.GL_EXT_mesh_shader || caps.GL_NV_mesh_shader
            if (!extensionAvailable || !profile.preferMeshShader || !RenderConfig.meshShaderEnabled()) {
                return null
            }
            val cache = GlMeshPipelineCache()
            return GlGpuDriver(cache.takeIf { it.supported })
        }
    }
}
