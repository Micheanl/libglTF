package com.micheanl.libgltf.integration.block

import com.micheanl.libgltf.api.GltfInstance
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3

/**
 * libgltf · GltfBlockEntityRenderProvider
 *
 * ```
 * private val provider: GltfBlockEntityRenderProvider<T>
 * ```
 *
 * 方块实体渲染提供者
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

interface GltfBlockEntityRenderProvider<T : BlockEntity> {
    fun instance(blockEntity: T): GltfInstance?

    fun extract(blockEntity: T, state: GltfBlockEntityRenderState, partialTicks: Float, cameraPosition: Vec3) {
        state.transform.identity()
    }
}
