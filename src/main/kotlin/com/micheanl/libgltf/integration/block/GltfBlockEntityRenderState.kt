package com.micheanl.libgltf.integration.block

import com.micheanl.libgltf.api.GltfInstance
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import org.joml.Matrix4f


/**
 * libgltf · GltfBlockEntityRenderState
 *
 * ```
 * override fun createRenderState(): GltfBlockEntityRenderState = GltfBlockEntityRenderState()
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GltfBlockEntityRenderState : BlockEntityRenderState() {
    var instance: GltfInstance? = null
    val transform: Matrix4f = Matrix4f()
}
