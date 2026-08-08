package com.micheanl.libgltf.integration.block

/**
 * libgltf · GltfBlockEntityRenderState
 *
 * <pre>{@code
 * override fun createRenderState(): GltfBlockEntityRenderState = GltfBlockEntityRenderState()
 * }</pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.api.GltfInstance
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import org.joml.Matrix4f

class GltfBlockEntityRenderState : BlockEntityRenderState() {
    var instance: GltfInstance? = null
    val transform: Matrix4f = Matrix4f()
}
