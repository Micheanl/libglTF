package com.micheanl.libgltf.integration.entity

import com.micheanl.libgltf.api.GltfInstance
import net.minecraft.world.entity.Entity




/**
 * libgltf · GltfEntityRenderProvider
 *
 * ```
 * private val provider: GltfEntityRenderProvider<T>
 * ```
 *
 * 实体渲染提供者
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

interface GltfEntityRenderProvider<T : Entity> {
    fun instance(entity: T): GltfInstance?

    fun extract(entity: T, state: GltfEntityRenderState, partialTicks: Float) {
        state.transform.identity()
    }
}
