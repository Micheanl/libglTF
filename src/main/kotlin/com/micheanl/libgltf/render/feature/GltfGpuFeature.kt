package com.micheanl.libgltf.render.feature

import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry
import net.minecraft.client.renderer.feature.FeatureRendererType

object GltfGpuFeature {
    val TYPE: FeatureRendererType<GltfGpuSubmit> = FeatureRendererType.create("libgltf_gpu")

    fun initialize() {
        FeatureRendererRegistry.register(TYPE, ::GltfGpuSubmitRenderer)
    }
}
