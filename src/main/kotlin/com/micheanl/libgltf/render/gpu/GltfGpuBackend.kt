package com.micheanl.libgltf.render.gpu

import com.micheanl.libgltf.mixin.FrontendGpuDeviceAccessor
import com.micheanl.libgltf.render.GltfGpuBackendType
import com.micheanl.libgltf.render.GltfGpuCapabilities
import com.micheanl.libgltf.render.GltfGpuPath
import com.micheanl.libgltf.render.vulkan.GltfRenderConfig
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.vertex.VertexFormat
import com.mojang.renderpearl.api.device.DeviceInfo
import com.mojang.renderpearl.backend.vulkan.VulkanDevice
import org.lwjgl.opengl.GL

object GltfGpuBackend {
    @Volatile
    private var vertexAttributeLimit = VertexFormat.MAX_VERTEX_ELEMENTS

    @Volatile
    private var cachedInfo: DeviceInfo? = null

    @Volatile
    private var vendorProfile: GltfGpuVendorProfile? = null

    @Volatile
    private var capabilities = GltfGpuCapabilities(
        GltfGpuBackendType.UNKNOWN,
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
        GltfGpuPath.CPU
    )

    fun refresh() {
        val device = RenderSystem.tryGetDevice() ?: return
        val info = device.deviceInfo
        cachedInfo = info
        val profile = GltfGpuVendors.profile(info)
        vendorProfile = profile
        val backend = when (info.backendName()) {
            "OpenGL" -> GltfGpuBackendType.OPENGL
            "Vulkan" -> GltfGpuBackendType.VULKAN
            else -> GltfGpuBackendType.UNKNOWN
        }
        val meshShader = when (backend) {
            GltfGpuBackendType.OPENGL -> {
                val gl = GL.getCapabilities()
                gl.GL_EXT_mesh_shader || gl.GL_NV_mesh_shader
            }
            GltfGpuBackendType.VULKAN -> info.underlyingExtensions().any { it.startsWith("VK_EXT_mesh_shader") }
            GltfGpuBackendType.UNKNOWN -> false
        }
        val features = info.features()
        val limit = ((device as FrontendGpuDeviceAccessor).libgltfBackend as? VertexAttributeLimitProvider)
            ?.maxVertexAttributes
            ?: VertexFormat.MAX_VERTEX_ELEMENTS
        val requiredAttributes = if (backend == GltfGpuBackendType.OPENGL) {
            GltfGpuFormats.REQUIRED_GL_VERTEX_ATTRIBUTES
        } else {
            GltfGpuFormats.REQUIRED_VERTEX_ATTRIBUTES
        }
        val instancing = backend != GltfGpuBackendType.UNKNOWN &&
            requiredAttributes <= limit
        val nativeMeshShader = backend == GltfGpuBackendType.VULKAN &&
            meshShader &&
            ((device as FrontendGpuDeviceAccessor).libgltfBackend as? VulkanDevice)?.vkDevice()?.capabilities?.VK_EXT_mesh_shader == true
        val nativeNvMeshShader = backend == GltfGpuBackendType.VULKAN &&
            ((device as FrontendGpuDeviceAccessor).libgltfBackend as? VulkanDevice)?.vkDevice()?.capabilities?.VK_NV_mesh_shader == true
        vertexAttributeLimit = limit
        capabilities = GltfGpuCapabilities(
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
            if (instancing) GltfGpuPath.INSTANCED else GltfGpuPath.CPU
        )
    }

    fun vertexAttributeLimit(): Int = vertexAttributeLimit

    fun capabilities(): GltfGpuCapabilities = capabilities

    fun deviceInfo(): DeviceInfo? = cachedInfo

    fun vendorProfile(): GltfGpuVendorProfile = vendorProfile ?: DEFAULT_VENDOR_PROFILE

    fun meshletBuilding(): Boolean = when (capabilities.backend) {
        GltfGpuBackendType.VULKAN -> true
        GltfGpuBackendType.OPENGL -> capabilities.meshShaderExtensionPresent
        GltfGpuBackendType.UNKNOWN -> false
    }

    private val DEFAULT_VENDOR_PROFILE = GltfGpuVendorProfile(
        GltfGpuVendor.UNKNOWN,
        true,
        true,
        true,
        true,
        65535,
        64
    )
}
