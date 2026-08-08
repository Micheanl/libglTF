package com.micheanl.libgltf.render.gpu

import com.micheanl.libgltf.mixin.FrontendGpuDeviceAccessor
import com.micheanl.libgltf.render.GpuBackendType
import com.micheanl.libgltf.render.GpuCapabilities
import com.micheanl.libgltf.render.GpuPath
import com.micheanl.libgltf.render.vulkan.RenderConfig
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.vertex.VertexFormat
import com.mojang.renderpearl.api.device.DeviceInfo
import com.mojang.renderpearl.backend.vulkan.VulkanDevice
import org.lwjgl.opengl.GL

object GpuBackend {
    @Volatile
    private var vertexAttributeLimit = VertexFormat.MAX_VERTEX_ELEMENTS

    @Volatile
    private var cachedInfo: DeviceInfo? = null

    @Volatile
    private var vendorProfile: GpuVendorProfile? = null

    @Volatile
    private var capabilities = GpuCapabilities(
        GpuBackendType.UNKNOWN,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        GpuPath.CPU
    )

    fun refresh() {
        val device = RenderSystem.tryGetDevice() ?: return
        val info = device.deviceInfo
        cachedInfo = info
        val profile = GpuVendors.profile(info)
        vendorProfile = profile
        val backend = when (info.backendName()) {
            "OpenGL" -> GpuBackendType.OPENGL
            "Vulkan" -> GpuBackendType.VULKAN
            else -> GpuBackendType.UNKNOWN
        }
        val meshShader = when (backend) {
            GpuBackendType.OPENGL -> {
                val gl = GL.getCapabilities()
                gl.GL_EXT_mesh_shader || gl.GL_NV_mesh_shader
            }
            GpuBackendType.VULKAN -> info.underlyingExtensions().any { it.startsWith("VK_EXT_mesh_shader") }
            GpuBackendType.UNKNOWN -> false
        }
        val features = info.features()
        val limit = ((device as FrontendGpuDeviceAccessor).libgltfBackend as? VertexAttributeLimitProvider)
            ?.maxVertexAttributes
            ?: VertexFormat.MAX_VERTEX_ELEMENTS
        val requiredAttributes = if (backend == GpuBackendType.OPENGL) {
            GpuFormats.REQUIRED_GL_VERTEX_ATTRIBUTES
        } else {
            GpuFormats.REQUIRED_VERTEX_ATTRIBUTES
        }
        val instancing = backend != GpuBackendType.UNKNOWN &&
            requiredAttributes <= limit
        val nativeMeshShader = backend == GpuBackendType.VULKAN &&
            meshShader &&
            ((device as FrontendGpuDeviceAccessor).libgltfBackend as? VulkanDevice)?.vkDevice()?.capabilities?.VK_EXT_mesh_shader == true
        val nativeNvMeshShader = backend == GpuBackendType.VULKAN &&
            ((device as FrontendGpuDeviceAccessor).libgltfBackend as? VulkanDevice)?.vkDevice()?.capabilities?.VK_NV_mesh_shader == true
        vertexAttributeLimit = limit
        capabilities = GpuCapabilities(
            backend,
            instancing,
            features.shaderDrawParameters(),
            features.multiDrawDirectInterleaved(),
            features.multiDrawDirectSeparate(),
            features.drawIndirect(),
            features.multiDrawIndirect(),
            features.nonZeroFirstInstance(),
            features.persistentMapping(),
            meshShader,
            nativeMeshShader,
            nativeNvMeshShader,
            if (instancing) GpuPath.INSTANCED else GpuPath.CPU
        )
    }

    fun vertexAttributeLimit(): Int = vertexAttributeLimit

    fun capabilities(): GpuCapabilities = capabilities

    fun deviceInfo(): DeviceInfo? = cachedInfo

    fun vendorProfile(): GpuVendorProfile = vendorProfile ?: DEFAULT_VENDOR_PROFILE

    fun meshletBuilding(): Boolean = when (capabilities.backend) {
        GpuBackendType.VULKAN -> true
        GpuBackendType.OPENGL -> capabilities.meshShaderExtensionPresent
        GpuBackendType.UNKNOWN -> false
    }

    private val DEFAULT_VENDOR_PROFILE = GpuVendorProfile(
        GpuVendor.UNKNOWN,
        true,
        true,
        true,
        true,
        65535,
        64
    )
}
