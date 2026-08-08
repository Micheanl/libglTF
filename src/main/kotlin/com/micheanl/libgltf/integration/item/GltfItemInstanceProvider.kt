package com.micheanl.libgltf.integration.item

import com.micheanl.libgltf.api.GltfInstance
import net.minecraft.world.item.ItemStack

/**
 * libgltf · GltfItemInstanceProvider
 *
 * ```
 * fun item(instance: GltfInstance, provider: GltfItemInstanceProvider): GltfItemRenderer =
 * ```
 *
 * 物品 glTF 渲染实例提供者
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

fun interface GltfItemInstanceProvider {
    fun instance(stack: ItemStack): GltfInstance?
}
