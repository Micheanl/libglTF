package com.micheanl.libgltf.render.gpu

import com.mojang.renderpearl.api.device.DeviceInfo

/**
 * libgltf · GpuVendor
 *
 * ```
 * GpuVendor.UNKNOWN,
 * ```
 *
 * 厂商识别与设备画像
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

enum class GpuVendor {
    NVIDIA,
    AMD,
    INTEL,
    APPLE,
    UNKNOWN
}

data class GpuVendorProfile(
    val vendor: GpuVendor,
    val preferMeshShader: Boolean,
    val preferVulkanMeshShader: Boolean,
    val enableInstanceCulling: Boolean,
    val enableMeshletCulling: Boolean,
    val maxTaskGroupCount: Int,
    val maxMeshWorkGroupSize: Int
)

object GpuVendors {
    fun profile(info: DeviceInfo): GpuVendorProfile {
        val vendor = vendor(info.vendorName())
        return when (vendor) {
            GpuVendor.NVIDIA -> GpuVendorProfile(
                vendor,
                true,
                true,
                true,
                true,
                65535,
                64
            )
            GpuVendor.AMD -> GpuVendorProfile(
                vendor,
                true,
                true,
                true,
                true,
                65535,
                64
            )
            GpuVendor.INTEL -> GpuVendorProfile(
                vendor,
                false,
                false,
                true,
                false,
                65535,
                64
            )
            GpuVendor.APPLE -> GpuVendorProfile(
                vendor,
                false,
                false,
                false,
                false,
                0,
                0
            )
            GpuVendor.UNKNOWN -> GpuVendorProfile(
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

    fun vendor(vendorName: String): GpuVendor {
        val name = vendorName.uppercase()
        return when {
            name.contains("NVIDIA") -> GpuVendor.NVIDIA
            name.contains("AMD") || name.contains("ATI") -> GpuVendor.AMD
            name.contains("INTEL") -> GpuVendor.INTEL
            name.contains("APPLE") -> GpuVendor.APPLE
            else -> GpuVendor.UNKNOWN
        }
    }
}
