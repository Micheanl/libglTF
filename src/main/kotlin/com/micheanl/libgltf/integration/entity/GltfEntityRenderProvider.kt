package com.micheanl.libgltf.integration.entity

/**
 * libgltf · GltfEntityRenderProvider
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import com.micheanl.libgltf.api.GltfInstance
import net.minecraft.world.entity.Entity

interface GltfEntityRenderProvider<T : Entity> {
    fun instance(entity: T): GltfInstance?

    fun extract(entity: T, state: GltfEntityRenderState, partialTicks: Float) {
        state.transform.identity()
    }
}
