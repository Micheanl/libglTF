package com.micheanl.libgltf.render

import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.api.GltfRenderMode
import com.micheanl.libgltf.material.TextureWrap
import com.micheanl.libgltf.model.GltfPrimitive
import com.micheanl.libgltf.model.PrimitiveMode
import com.micheanl.libgltf.render.gpu.GltfGpuBackend
import com.micheanl.libgltf.render.iris.IrisCompat
import com.mojang.blaze3d.vertex.PoseStack
import java.util.function.Consumer
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases
import net.minecraft.client.renderer.OrderedSubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector3fc

object GltfSceneRenderer {
    fun submit(
        instance: GltfInstance,
        poseStack: PoseStack,
        submitNodeCollector: OrderedSubmitNodeCollector,
        light: Int,
        overlay: Int = OverlayTexture.NO_OVERLAY,
        distanceSquared: Float = 0.0f,
        transform: Matrix4fc = instance.transform
    ) {
        if (!instance.visible || instance.handle.isClosed) return
        if (!instance.lodSelector.visible(distanceSquared)) return
        instance.lodLevel = instance.lodSelector.select(distanceSquared, instance.lodLevel)
        if (instance.lodSelector.animate(distanceSquared)) instance.updateAnimationAt(System.nanoTime()) else instance.syncAnimation()
        val resource = GltfRenderSystem.resource(instance.handle.resourceId) ?: return
        val textures = resource.textures()
        val gpuEnabled =
            instance.renderMode != GltfRenderMode.CPU &&
                GltfGpuBackend.capabilities().instancing &&
                !IrisCompat.shaderPackActive()
        val asset = instance.handle.asset
        poseStack.pushPose()
        poseStack.mulPose(transform)
        for (nodeIndex in asset.topologicalOrder) {
            val node = asset.nodes[nodeIndex]
            val meshIndex = node.meshIndex
            if (meshIndex < 0) continue
            val mesh = asset.meshes[meshIndex]
            val renderers = instance.geometryRenderers[nodeIndex]
            val gpuSubmits = instance.gpuSubmits[nodeIndex]
            for (primitiveIndex in mesh.primitives.indices) {
                val primitive = mesh.primitives[primitiveIndex]
                val renderer = renderers[primitiveIndex]
                if (renderer.transparent() && !instance.lodSelector.transparent(distanceSquared)) continue
                if (gpuEnabled && gpuCompatible(instance, nodeIndex, primitive)) {
                    val gpuResources = resource.gpu()
                    if (!gpuResources.failed(meshIndex, primitiveIndex)) {
                        val submit = gpuSubmits[primitiveIndex]
                        submit.configure(resource, textures, light, overlay, poseStack.last().pose())
                        if (renderer.transparent()) {
                            submitNodeCollector.submitCustom(SubmitRenderPhases.TRANSLUCENT_MODELS, submit)
                        } else {
                            submitNodeCollector.submitCustom(SubmitRenderPhases.SOLID, submit)
                        }
                        continue
                    }
                }
                renderer.light = light
                renderer.overlay = overlay
                poseStack.pushPose()
                if (node.skinIndex < 0) poseStack.mulPose(instance.animation.pose.globalMatrices[nodeIndex])
                submitNodeCollector.submitCustomGeometry(poseStack, renderer.renderType(resource, textures), renderer)
                poseStack.popPose()
            }
        }
        poseStack.popPose()
    }

    fun submitGlint(
        instance: GltfInstance,
        poseStack: PoseStack,
        submitNodeCollector: OrderedSubmitNodeCollector,
        light: Int,
        overlay: Int = OverlayTexture.NO_OVERLAY,
        transform: Matrix4fc = instance.transform
    ) {
        if (!instance.visible || instance.handle.isClosed) return
        val asset = instance.handle.asset
        poseStack.pushPose()
        poseStack.mulPose(transform)
        for (nodeIndex in asset.topologicalOrder) {
            val node = asset.nodes[nodeIndex]
            if (node.meshIndex < 0) continue
            val renderers = instance.geometryRenderers[nodeIndex]
            for (renderer in renderers) {
                renderer.light = light
                renderer.overlay = overlay
                poseStack.pushPose()
                if (node.skinIndex < 0) poseStack.mulPose(instance.animation.pose.globalMatrices[nodeIndex])
                submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityGlint(), renderer)
                poseStack.popPose()
            }
        }
        poseStack.popPose()
    }

    fun getExtents(instance: GltfInstance, output: Consumer<Vector3fc>) {
        val bounds = instance.handle.asset.bounds
        for (x in 0..1) {
            for (y in 0..1) {
                for (z in 0..1) {
                    val point = Vector3f(bounds[x * 3], bounds[y * 3 + 1], bounds[z * 3 + 2])
                    instance.transform.transformPosition(point)
                    output.accept(point)
                }
            }
        }
    }

    private fun gpuCompatible(instance: GltfInstance, nodeIndex: Int, primitive: GltfPrimitive): Boolean {
        if (primitive.mode != PrimitiveMode.TRIANGLES || primitive.morphTargetCount > 0) return false
        val asset = instance.handle.asset
        val node = asset.nodes[nodeIndex]
        if (primitive.skin != null && node.skinIndex < 0) return false
        val sourceMaterialIndex = primitive.materialIndex.coerceIn(0, asset.materials.lastIndex)
        val material = asset.materials[instance.resolveMaterial(sourceMaterialIndex)]
        if (instance.materialOverrides[sourceMaterialIndex]?.baseColorIdentifier != null) return true
        val overrideTexture = instance.materialOverrides[sourceMaterialIndex]?.baseColorTextureIndex ?: -1
        val textureIndex = if (overrideTexture >= 0) overrideTexture else material.baseColorTexture?.textureIndex ?: -1
        val sampler = asset.textures.getOrNull(textureIndex)?.sampler ?: return true
        return sampler.wrapS != TextureWrap.MIRRORED_REPEAT && sampler.wrapT != TextureWrap.MIRRORED_REPEAT
    }
}
