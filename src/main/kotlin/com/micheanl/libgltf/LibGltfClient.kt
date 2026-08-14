package com.micheanl.libgltf

import com.micheanl.libgltf.render.GltfRenderSystem
import net.fabricmc.api.ClientModInitializer

/**
 * libgltf · LibGltfClient
 *
 * ```
 * object LibGltfClient : ClientModInitializer
 * ```
 *
 * 客户端侧初始化与生命周期
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object LibGltfClient : ClientModInitializer {
    override fun onInitializeClient() {
        GltfRenderSystem.initialize()
    }
}
