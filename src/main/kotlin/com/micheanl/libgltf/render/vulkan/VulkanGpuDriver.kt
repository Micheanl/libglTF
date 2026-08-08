package com.micheanl.libgltf.render.vulkan

import com.micheanl.libgltf.mixin.FrontendGpuDeviceAccessor
import com.micheanl.libgltf.render.GpuBackendType
import com.micheanl.libgltf.render.gpu.GpuDriver
import com.micheanl.libgltf.render.gpu.GpuBackend
import com.micheanl.libgltf.render.gpu.OcclusionDepth
import com.mojang.renderpearl.api.device.GpuDevice
import com.mojang.renderpearl.backend.vulkan.VulkanDevice


/**
 * libgltf · VulkanGpuDriver
 *
 * ```
 * gpuDriven = VulkanGpuDriver.create(RenderSystem.getDevice()) ?: GlGpuDriver.create()
 * ```
 *
 * Vulkan 后端的 GPU 驱动
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class VulkanGpuDriver private constructor(
    val pipeline: VulkanComputePipeline,
    val meshPipelines: VulkanMeshCache?
) : GpuDriver {
    override val meshSupported: Boolean
        get() = meshPipelines?.supported == true

    override fun close() {
        meshPipelines?.close()
        pipeline.close()
    }

    companion object {
        fun create(device: GpuDevice): VulkanGpuDriver? {
            val capabilities = GpuBackend.capabilities()
            if (!RenderConfig.enabled ||
                capabilities.backend != GpuBackendType.VULKAN ||
                !capabilities.drawIndirect ||
                !capabilities.multiDrawIndirect ||
                !capabilities.nonZeroFirstInstance
            ) return null
            val backend = (device as FrontendGpuDeviceAccessor).libgltfBackend as? VulkanDevice ?: return null
            val profile = GpuBackend.vendorProfile()
            val mesh = if (
                profile.preferMeshShader &&
                RenderConfig.meshShaderEnabled()
            ) {
                if (capabilities.meshShaderNvActive) {
                    VulkanNvMeshPipelineCache(backend).takeIf { it.supported }
                } else if (backend.vkDevice().capabilities.VK_EXT_mesh_shader) {
                    VulkanMeshPipelineCache(backend).takeIf { it.supported }
                } else {
                    null
                }
            } else {
                null
            }
            OcclusionDepth.ensureCreated()
            return VulkanGpuDriver(VulkanComputePipeline.create(backend), mesh)
        }
    }
}
