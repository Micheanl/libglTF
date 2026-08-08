package com.micheanl.libgltf.render.gpu

import com.micheanl.libgltf.render.vulkan.GltfGpuDrivenSettings
import com.micheanl.libgltf.render.GltfGpuBackendType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.GpuTextureView
import net.minecraft.client.Minecraft

object GltfOcclusionDepth : AutoCloseable {
    private var target: TextureTarget? = null
    private var retired: TextureTarget? = null

    fun ensureCreated() {
        if (!active()) return
        val main = Minecraft.getInstance().gameRenderer.mainRenderTarget() ?: return
        val current = target
        if (current == null || current.width != main.width || current.height != main.height) {
            val created = TextureTarget("libgltf occlusion depth", main.width, main.height, null, GpuFormat.D32_FLOAT)
            created.getDepthTexture()?.let {
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(it, 1.0)
            }
            target = created
            if (current != null) {
                retired?.destroyBuffers()
                retired = current
            }
        }
    }

    fun update() {
        if (!active()) return
        ensureCreated()
        val main = Minecraft.getInstance().gameRenderer.mainRenderTarget() ?: return
        target?.copyDepthFrom(main)
    }

    fun view(): GpuTextureView? = target?.getDepthTextureView()

    fun texture(): GpuTexture? = target?.getDepthTexture()

    override fun close() {
        retired?.destroyBuffers()
        retired = null
        target?.destroyBuffers()
        target = null
    }

    private fun active(): Boolean =
        GltfGpuDrivenSettings.occlusionCulling && GltfGpuBackend.capabilities().backend == GltfGpuBackendType.VULKAN
}
