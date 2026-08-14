package com.micheanl.libgltf.integration.entity

import com.micheanl.libgltf.render.GltfSceneRenderer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.Entity

/**
 * libgltf · GltfEntityRenderer
 *
 * ```
 * ): GltfEntityRenderer<T> = GltfEntityRenderer(context, provider)
 * ```
 *
 * 实体 glTF 渲染器
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GltfEntityRenderer<T : Entity>(
    context: EntityRendererProvider.Context,
    private val provider: GltfEntityRenderProvider<T>
) : EntityRenderer<T, GltfEntityRenderState>(context) {
    override fun createRenderState(): GltfEntityRenderState = GltfEntityRenderState()

    override fun extractRenderState(entity: T, state: GltfEntityRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        state.instance = provider.instance(entity)
        state.transform.identity()
        provider.extract(entity, state, partialTicks)
    }

    override fun submit(
        state: GltfEntityRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val instance = state.instance
        if (instance != null && !state.isInvisible) {
            poseStack.pushPose()
            poseStack.mulPose(state.transform)
            GltfSceneRenderer.submit(
                instance,
                poseStack,
                submitNodeCollector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.distanceToCameraSq.toFloat()
            )
            poseStack.popPose()
        }
        super.submit(state, poseStack, submitNodeCollector, camera)
    }
}
