package com.micheanl.libgltf.integration

/**
 * libgltf · GltfRenderers
 *
 * 集成渲染器注册
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.integration.block.GltfBlockEntityRenderProvider
import com.micheanl.libgltf.integration.block.GltfBlockEntityRenderer
import com.micheanl.libgltf.integration.block.GltfBlockRenderer
import com.micheanl.libgltf.integration.entity.GltfEntityRenderProvider
import com.micheanl.libgltf.integration.entity.GltfEntityRenderer
import com.micheanl.libgltf.integration.item.GltfItemInstanceProvider
import com.micheanl.libgltf.integration.item.GltfItemRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity

object GltfRenderers {
    @JvmStatic
    fun item(instance: GltfInstance): GltfItemRenderer = GltfItemRenderer(instance)

    @JvmStatic
    fun item(instance: GltfInstance, provider: GltfItemInstanceProvider): GltfItemRenderer =
        GltfItemRenderer(instance, provider)

    @JvmStatic
    fun block(instance: GltfInstance): GltfBlockRenderer = GltfBlockRenderer(instance)

    @JvmStatic
    fun <T : Entity> entity(
        context: EntityRendererProvider.Context,
        provider: GltfEntityRenderProvider<T>
    ): GltfEntityRenderer<T> = GltfEntityRenderer(context, provider)

    @JvmStatic
    fun <T : BlockEntity> blockEntity(
        provider: GltfBlockEntityRenderProvider<T>
    ): GltfBlockEntityRenderer<T> = GltfBlockEntityRenderer(provider)
}
