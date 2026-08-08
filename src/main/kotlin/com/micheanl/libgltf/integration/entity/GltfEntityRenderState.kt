package com.micheanl.libgltf.integration.entity

import com.micheanl.libgltf.api.GltfInstance
import net.minecraft.client.renderer.entity.state.EntityRenderState
import org.joml.Matrix4f




/**
 * libgltf · GltfEntityRenderState
 *
 * ```
 * ) : EntityRenderer<T, GltfEntityRenderState>(context) {
 * ```
 *
 * 实体渲染状态
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GltfEntityRenderState : EntityRenderState() {
    var instance: GltfInstance? = null
    val transform: Matrix4f = Matrix4f()
}
