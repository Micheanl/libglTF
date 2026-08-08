package com.micheanl.libgltf.api

import com.micheanl.libgltf.asset.GltfLoadResult
import com.micheanl.libgltf.asset.GltfLoader
import com.micheanl.libgltf.lod.LodPolicy
import com.micheanl.libgltf.model.GltfAsset
import com.micheanl.libgltf.render.GpuCapabilities
import com.micheanl.libgltf.render.GltfRenderRegistry
import com.micheanl.libgltf.render.GltfRenderSystem
import com.micheanl.libgltf.render.GltfSceneRenderer
import com.micheanl.libgltf.render.gpu.GpuBackend
import com.mojang.blaze3d.vertex.PoseStack
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import net.minecraft.client.renderer.OrderedSubmitNodeCollector




/**
 * libgltf · GltfApiImpl
 *
 * ```
 * object GltfApiImpl : GltfApi {
 * ```
 *
 * GltfApi 的默认实现
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object GltfApiImpl : GltfApi {
    override fun load(path: Path, lodPolicy: LodPolicy): GltfLoadResult = GltfLoader.load(path, lodPolicy)

    override fun loadAsync(path: Path, lodPolicy: LodPolicy): CompletableFuture<GltfLoadResult> = GltfLoader.loadAsync(path, lodPolicy)

    override fun upload(asset: GltfAsset): GltfHandle = GltfRenderSystem.upload(asset)

    override fun createInstance(handle: GltfHandle): GltfInstance {
        require(!handle.isClosed)
        return GltfInstance(handle)
    }

    override fun register(instance: GltfInstance): GltfInstanceId = GltfRenderRegistry.register(instance)

    override fun unregister(id: GltfInstanceId): Boolean = GltfRenderRegistry.unregister(id)

    override fun gpuCapabilities(): GpuCapabilities = GpuBackend.capabilities()

    override fun submit(
        instance: GltfInstance,
        poseStack: PoseStack,
        submitNodeCollector: OrderedSubmitNodeCollector,
        light: Int,
        overlay: Int,
        distanceSquared: Float
    ) {
        GltfSceneRenderer.submit(instance, poseStack, submitNodeCollector, light, overlay, distanceSquared)
    }
}
