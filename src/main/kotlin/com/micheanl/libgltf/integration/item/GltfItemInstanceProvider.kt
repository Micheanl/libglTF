package com.micheanl.libgltf.integration.item

/**
 * libgltf · GltfItemInstanceProvider
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.api.GltfInstance
import net.minecraft.world.item.ItemStack

fun interface GltfItemInstanceProvider {
    fun instance(stack: ItemStack): GltfInstance?
}
