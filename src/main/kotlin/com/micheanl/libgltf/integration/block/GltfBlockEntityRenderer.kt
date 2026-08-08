package com.micheanl.libgltf.integration.block

/**
 * libgltf · GltfBlockEntityRenderer
 *
 * <pre><code>
 * ): GltfBlockEntityRenderer&lt;T&gt; = GltfBlockEntityRenderer(provider)
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.render.GltfSceneRenderer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3

class GltfBlockEntityRenderer<T : BlockEntity>(
    private val provider: GltfBlockEntityRenderProvider<T>
) : BlockEntityRenderer<T, GltfBlockEntityRenderState> {
    override fun createRenderState(): GltfBlockEntityRenderState = GltfBlockEntityRenderState()

    override fun extractRenderState(
        blockEntity: T,
        state: GltfBlockEntityRenderState,
        partialTicks: Float,
        cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        super<BlockEntityRenderer>.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress)
        state.instance = provider.instance(blockEntity)
        state.transform.identity()
        provider.extract(blockEntity, state, partialTicks, cameraPosition)
    }

    override fun submit(
        state: GltfBlockEntityRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val instance = state.instance ?: return
        val x = state.blockPos.x + 0.5 - camera.pos.x
        val y = state.blockPos.y + 0.5 - camera.pos.y
        val z = state.blockPos.z + 0.5 - camera.pos.z
        poseStack.pushPose()
        poseStack.mulPose(state.transform)
        GltfSceneRenderer.submit(
            instance,
            poseStack,
            submitNodeCollector,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            (x * x + y * y + z * z).toFloat()
        )
        poseStack.popPose()
    }
}
