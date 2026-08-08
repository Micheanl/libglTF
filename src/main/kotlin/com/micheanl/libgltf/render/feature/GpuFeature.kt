package com.micheanl.libgltf.render.feature

import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry
import net.minecraft.client.renderer.feature.FeatureRendererType


/**
 * libgltf · GpuFeature
 *
 * ```
 * GpuFeature.initialize()
 * ```
 *
 * GPU FeatureRenderer 注册
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object GpuFeature {
    val TYPE: FeatureRendererType<GpuSubmit> = FeatureRendererType.create("libgltf_gpu")

    fun initialize() {
        FeatureRendererRegistry.register(TYPE, ::GpuSubmitRenderer)
    }
}
