package com.micheanl.libgltf.integration.item

import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.render.GltfSceneRenderer
import com.mojang.blaze3d.vertex.PoseStack
import java.util.function.Consumer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.special.SpecialModelRenderer
import net.minecraft.world.item.ItemStack
import org.joml.Vector3fc




/**
 * libgltf · GltfItemRenderer
 *
 * ```
 * fun item(instance: GltfInstance): GltfItemRenderer = GltfItemRenderer(instance)
 * ```
 *
 * 物品 glTF 渲染器
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GltfItemRenderer(
    private val boundsInstance: GltfInstance,
    private val provider: GltfItemInstanceProvider = GltfItemInstanceProvider { boundsInstance }
) : SpecialModelRenderer<GltfInstance> {
    override fun extractArgument(stack: ItemStack): GltfInstance? = provider.instance(stack)

    override fun submit(
        argument: GltfInstance?,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        lightCoords: Int,
        overlayCoords: Int,
        hasFoil: Boolean,
        outlineColor: Int
    ) {
        val instance = argument ?: return
        GltfSceneRenderer.submit(
            instance,
            poseStack,
            submitNodeCollector.order(0),
            lightCoords,
            overlayCoords
        )
        if (hasFoil) {
            GltfSceneRenderer.submitGlint(
                instance,
                poseStack,
                submitNodeCollector.order(1),
                lightCoords,
                overlayCoords
            )
        }
    }

    override fun getExtents(output: Consumer<Vector3fc>) {
        GltfSceneRenderer.getExtents(boundsInstance, output)
    }
}
