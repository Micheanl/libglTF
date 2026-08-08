package com.micheanl.libgltf

import com.micheanl.libgltf.render.GltfRenderSystem
import net.fabricmc.api.ClientModInitializer

object LibGltfClient : ClientModInitializer {
    override fun onInitializeClient() {
        GltfRenderSystem.initialize()
    }
}
