package com.micheanl.libgltf.render.gpu

/**
 * libgltf · OcclusionDepth
 *
 * 上一帧深度遮挡纹理与复制
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import com.micheanl.libgltf.render.vulkan.RenderConfig
import com.micheanl.libgltf.render.GpuBackendType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.GpuTextureView
import net.minecraft.client.Minecraft

object OcclusionDepth : AutoCloseable {
    private var target: TextureTarget? = null
    private var retired: TextureTarget? = null
    @Volatile
    var generation: Long = 0
        private set

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
            generation++
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
        RenderConfig.occlusionCulling && GpuBackend.capabilities().backend == GpuBackendType.VULKAN
}
