package com.micheanl.libgltf.render.gpu

import com.mojang.renderpearl.api.device.DeviceInfo

enum class GltfGpuVendor {
    NVIDIA,
    AMD,
    INTEL,
    APPLE,
    UNKNOWN
}

data class GltfGpuVendorProfile(
    val vendor: GltfGpuVendor,
    val preferMeshShader: Boolean,
    val preferVulkanMeshShader: Boolean,
    val enableInstanceCulling: Boolean,
    val enableMeshletCulling: Boolean,
    val maxTaskGroupCount: Int,
    val maxMeshWorkGroupSize: Int
)

object GltfGpuVendors {
    fun profile(info: DeviceInfo): GltfGpuVendorProfile {
        val vendor = vendor(info.vendorName())
        return when (vendor) {
            GltfGpuVendor.NVIDIA -> GltfGpuVendorProfile(
                vendor,
                true,
                false,
                true,
                true,
                65535,
                64
            )
            GltfGpuVendor.AMD -> GltfGpuVendorProfile(
                vendor,
                true,
                true,
                true,
                true,
                65535,
                64
            )
            GltfGpuVendor.INTEL -> GltfGpuVendorProfile(
                vendor,
                false,
                false,
                true,
                false,
                65535,
                64
            )
            GltfGpuVendor.APPLE -> GltfGpuVendorProfile(
                vendor,
                false,
                false,
                false,
                false,
                0,
                0
            )
            GltfGpuVendor.UNKNOWN -> GltfGpuVendorProfile(
                vendor,
                true,
                true,
                true,
                true,
                65535,
                64
            )
        }
    }

    fun vendor(vendorName: String): GltfGpuVendor {
        val name = vendorName.uppercase()
        return when {
            name.contains("NVIDIA") -> GltfGpuVendor.NVIDIA
            name.contains("AMD") || name.contains("ATI") -> GltfGpuVendor.AMD
            name.contains("INTEL") -> GltfGpuVendor.INTEL
            name.contains("APPLE") -> GltfGpuVendor.APPLE
            else -> GltfGpuVendor.UNKNOWN
        }
    }
}
