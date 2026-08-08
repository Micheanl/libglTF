package com.micheanl.libgltf.render.feature

import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry
import net.minecraft.client.renderer.feature.FeatureRendererType

object GpuFeature {
    val TYPE: FeatureRendererType<GpuSubmit> = FeatureRendererType.create("libgltf_gpu")

    fun initialize() {
        FeatureRendererRegistry.register(TYPE, ::GpuSubmitRenderer)
    }
}
