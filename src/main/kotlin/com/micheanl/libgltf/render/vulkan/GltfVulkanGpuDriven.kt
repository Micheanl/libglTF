package com.micheanl.libgltf.render.vulkan

import com.micheanl.libgltf.mixin.FrontendGpuDeviceAccessor
import com.micheanl.libgltf.render.GltfGpuBackendType
import com.micheanl.libgltf.render.gpu.GltfGpuDriver
import com.micheanl.libgltf.render.gpu.GltfGpuBackend
import com.micheanl.libgltf.render.gpu.GltfOcclusionDepth
import com.mojang.renderpearl.api.device.GpuDevice
import com.mojang.renderpearl.backend.vulkan.VulkanDevice

class GltfVulkanGpuDriven private constructor(
    val pipeline: GltfVulkanComputePipeline,
    val meshPipelines: GltfVulkanMeshCache?
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
                GltfGpuDrivenSettings.meshShaderEnabled()
            ) {
                if (capabilities.meshShaderNvActive) {
                    GltfVulkanNvMeshPipelineCache(backend).takeIf { it.supported }
                } else if (backend.vkDevice().capabilities.VK_EXT_mesh_shader) {
                    GltfVulkanMeshPipelineCache(backend).takeIf { it.supported }
                } else {
                    null
                }
            } else {
                null
            }
            GltfOcclusionDepth.ensureCreated()
            return GltfVulkanGpuDriven(GltfVulkanComputePipeline.create(backend), mesh)
        }
    }
}
