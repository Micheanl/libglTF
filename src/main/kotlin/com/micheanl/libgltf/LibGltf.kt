package com.micheanl.libgltf

import com.micheanl.libgltf.api.GltfApi
import com.micheanl.libgltf.api.GltfApiImpl
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier


/**
 * libgltf · LibGltf
 *
 * <pre><code>
 * LibGltf.id("runtime/$resourceId/texture_$index")
 * </code></pre>
 *
 * 模组主入口与资源 ID 定义
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object LibGltf : ModInitializer {
    const val MOD_ID: String = "libgltf"

    @JvmField
    val api: GltfApi = GltfApiImpl

    override fun onInitialize() = Unit

    @JvmStatic
    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
