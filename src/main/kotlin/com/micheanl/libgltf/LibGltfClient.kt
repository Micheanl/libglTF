package com.micheanl.libgltf

/**
 * libgltf · LibGltfClient
 *
 * 客户端侧初始化与生命周期
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import com.micheanl.libgltf.render.GltfRenderSystem
import net.fabricmc.api.ClientModInitializer

object LibGltfClient : ClientModInitializer {
    override fun onInitializeClient() {
        GltfRenderSystem.initialize()
    }
}
