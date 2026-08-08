package com.micheanl.libgltf.render.vulkan

import com.micheanl.libgltf.mixin.FrontendGpuDeviceAccessor
import com.micheanl.libgltf.render.GltfGpuBackendType
import com.micheanl.libgltf.render.gpu.GltfGpuDriver
import com.micheanl.libgltf.render.gpu.GltfGpuBackend
import com.mojang.renderpearl.api.device.GpuDevice
import com.mojang.renderpearl.backend.vulkan.VulkanDevice
import com.mojang.logging.LogUtils

class GltfVulkanGpuDriven private constructor(
    val pipeline: GltfVulkanComputePipeline,
    val meshPipelines: GltfVulkanMeshPipelineCache?
) : GltfGpuDriver {
    override val meshSupported: Boolean
        get() = meshPipelines?.supported == true

    override fun close() {
        meshPipelines?.close()
        pipeline.close()
    }

    companion object {
        fun create(device: GpuDevice): GltfVulkanGpuDriven? {
            val capabilities = GltfGpuBackend.capabilities()
            if (!GltfGpuDrivenSettings.enabled ||
                capabilities.backend != GltfGpuBackendType.VULKAN ||
                !capabilities.drawIndirect ||
                !capabilities.multiDrawIndirect ||
                !capabilities.nonZeroFirstInstance
            ) return null
            val backend = (device as FrontendGpuDeviceAccessor).libgltfBackend as? VulkanDevice ?: return null
            val profile = GltfGpuBackend.vendorProfile()
            val mesh = if (
                profile.preferMeshShader &&
                GltfGpuDrivenSettings.meshShader &&
                backend.vkDevice().capabilities.VK_EXT_mesh_shader
            ) {
                GltfVulkanMeshPipelineCache(backend).also {
                    LOGGER.info("libgltf Vulkan mesh shader enabled vendor={} supported={}", profile.vendor, it.supported)
                }.takeIf { it.supported }
            } else {
                LOGGER.info(
                    "libgltf Vulkan mesh shader disabled vendor={} enabled={} extension={}",
                    profile.vendor,
                    GltfGpuDrivenSettings.meshShader,
                    backend.vkDevice().capabilities.VK_EXT_mesh_shader
                )
                null
            }
            return GltfVulkanGpuDriven(GltfVulkanComputePipeline.create(backend), mesh)
        }

        private val LOGGER = LogUtils.getLogger()
    }
}
