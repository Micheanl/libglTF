package com.micheanl.libgltf.integration.block

/**
 * libgltf · GltfBlockRenderer
 *
 * ```
 * fun block(instance: GltfInstance): GltfBlockRenderer = GltfBlockRenderer(instance)
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.render.GltfSceneRenderer
import com.mojang.blaze3d.vertex.PoseStack
import java.util.function.Consumer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer
import org.joml.Vector3fc

class GltfBlockRenderer(private val instance: GltfInstance) : NoDataSpecialModelRenderer {
    override fun submit(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        lightCoords: Int,
        overlayCoords: Int,
        hasFoil: Boolean,
        outlineColor: Int
    ) {
        GltfSceneRenderer.submit(
            instance,
            poseStack,
            submitNodeCollector,
            lightCoords,
            overlayCoords
        )
    }

    override fun getExtents(output: Consumer<Vector3fc>) {
        GltfSceneRenderer.getExtents(instance, output)
    }
}
