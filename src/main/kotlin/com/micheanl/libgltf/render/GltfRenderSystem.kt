package com.micheanl.libgltf.render

import com.micheanl.libgltf.api.GltfHandle
import com.micheanl.libgltf.model.GltfAsset
import com.micheanl.libgltf.render.feature.GltfGpuFeature
import com.micheanl.libgltf.render.gpu.GltfGpuBackend
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object GltfRenderSystem {
    private val initialized = AtomicBoolean()
    private val nextResourceId = AtomicLong(1L)
    private val resources = ConcurrentHashMap<Long, GltfRenderAsset>()

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        GltfConfig.load()
        GltfGpuBackend.refresh()
        ClientLifecycleEvents.CLIENT_STARTED.register { _ -> GltfGpuBackend.refresh() }
        ClientLifecycleEvents.CLIENT_STOPPING.register { _ -> closeAll() }
        GltfGpuFeature.initialize()
        LevelExtractionEvents.END_EXTRACTION.register { GltfFrameState.capture() }
        LevelRenderEvents.COLLECT_SUBMITS.register(GltfWorldRenderer::submit)
    }

    fun upload(asset: GltfAsset): GltfHandle {
        val id = nextResourceId.getAndIncrement()
        resources[id] = GltfRenderAsset(id, asset)
        return GltfHandle(asset, id)
    }

    fun release(resourceId: Long) {
        GltfRenderRegistry.removeByResource(resourceId)
        GltfRenderTypes.remove(resourceId)
        val resource = resources.remove(resourceId) ?: return
        val close = { RenderSystem.queueFencedTask(resource::close) }
        if (RenderSystem.isOnRenderThread()) close() else Minecraft.getInstance().execute(close)
    }

    private fun closeAll() {
        for ((id, resource) in resources) {
            if (!resources.remove(id, resource)) continue
            GltfRenderRegistry.removeByResource(id)
            GltfRenderTypes.remove(id)
            resource.close()
        }
    }

    fun resource(resourceId: Long): GltfRenderAsset? = resources[resourceId]
}
