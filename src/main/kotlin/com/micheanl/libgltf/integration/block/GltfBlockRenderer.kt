package com.micheanl.libgltf.integration.block

import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.render.GltfSceneRenderer
import com.mojang.blaze3d.vertex.PoseStack
import java.util.function.Consumer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer
import org.joml.Vector3fc

/**
 * libgltf · GltfBlockRenderer
 *
 * ```
 * fun block(instance: GltfInstance): GltfBlockRenderer = GltfBlockRenderer(instance)
 * ```
 *
 * 方块 glTF 渲染器
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

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
