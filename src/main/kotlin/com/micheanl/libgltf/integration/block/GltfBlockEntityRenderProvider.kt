package com.micheanl.libgltf.integration.block

/**
 * libgltf · GltfBlockEntityRenderProvider
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import com.micheanl.libgltf.api.GltfInstance
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3

interface GltfBlockEntityRenderProvider<T : BlockEntity> {
    fun instance(blockEntity: T): GltfInstance?

    fun extract(blockEntity: T, state: GltfBlockEntityRenderState, partialTicks: Float, cameraPosition: Vec3) {
        state.transform.identity()
    }
}
