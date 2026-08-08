package com.micheanl.libgltf.api

import com.micheanl.libgltf.asset.GltfLoadResult
import com.micheanl.libgltf.lod.LodPolicy
import com.micheanl.libgltf.model.GltfAsset
import com.micheanl.libgltf.render.GpuCapabilities
import com.mojang.blaze3d.vertex.PoseStack
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import net.minecraft.client.renderer.OrderedSubmitNodeCollector
import net.minecraft.client.renderer.texture.OverlayTexture

interface GltfApi {
    fun load(path: Path, lodPolicy: LodPolicy = LodPolicy.DEFAULT): GltfLoadResult
    fun loadAsync(path: Path, lodPolicy: LodPolicy = LodPolicy.DEFAULT): CompletableFuture<GltfLoadResult>
    fun upload(asset: GltfAsset): GltfHandle
    fun createInstance(handle: GltfHandle): GltfInstance
    fun register(instance: GltfInstance): GltfInstanceId
    fun unregister(id: GltfInstanceId): Boolean
    fun gpuCapabilities(): GpuCapabilities
    fun submit(
        instance: GltfInstance,
        poseStack: PoseStack,
        submitNodeCollector: OrderedSubmitNodeCollector,
        light: Int,
        overlay: Int = OverlayTexture.NO_OVERLAY,
        distanceSquared: Float = 0.0f
    )
}
