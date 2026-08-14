package com.micheanl.libgltf.render

import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.api.GltfRenderMode
import com.micheanl.libgltf.material.TextureWrap
import com.micheanl.libgltf.mixin.FrustumAccessor
import com.micheanl.libgltf.model.GltfPrimitive
import com.micheanl.libgltf.model.PrimitiveMode
import com.micheanl.libgltf.render.cpu.GltfGeometryRenderer
import com.micheanl.libgltf.render.feature.GpuSubmit
import com.micheanl.libgltf.render.gpu.GpuBackend
import com.micheanl.libgltf.render.iris.IrisCompat
import com.mojang.blaze3d.vertex.PoseStack
import java.util.function.Consumer
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OrderedSubmitNodeCollector
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.rendertype.RenderTypes
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * libgltf · GltfSceneRenderer
 *
 * ```
 * GltfSceneRenderer.submit(instance, poseStack, submitNodeCollector, light, overlay, distanceSquared)
 * ```
 *
 * 场景实例收集与提交入口
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object GltfSceneRenderer {
    private val cullMatrix = Matrix4f()
    private val cullPoint = Vector3f()
    private val instanceMatrix = Matrix4f()

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
                GpuBackend.capabilities().instancing &&
                !IrisCompat.shaderPackActive()
        val asset = instance.handle.asset
        val camera = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState
        val frustumCulling = camera.isFrustumCaptured
        poseStack.pushPose()
        poseStack.mulPose(transform)
        for (nodeIndex in asset.topologicalOrder) {
            if (!instance.sceneMask[nodeIndex]) continue
            val node = asset.nodes[nodeIndex]
            val meshLods = node.meshLods
            val lodCount = if (meshLods.isNotEmpty()) meshLods.size else 1
            val nodeLod = instance.lodLevel.coerceIn(0, lodCount - 1)
            val meshIndex = if (meshLods.isNotEmpty()) meshLods[nodeLod] else node.meshIndex
            if (meshIndex < 0) continue
            val mesh = asset.meshes[meshIndex]
            val renderers = instance.geometryRenderers[nodeIndex][nodeLod]
            val gpuSubmits = instance.gpuSubmits[nodeIndex][nodeLod]
            for (primitiveIndex in mesh.primitives.indices) {
                val primitive = mesh.primitives[primitiveIndex]
                val renderer = renderers[primitiveIndex]
                if (
                    frustumCulling &&
                    !(node.skinIndex >= 0 && primitive.skin != null) &&
                    primitive.morphTargetCount == 0 &&
                    !nodeVisible(
                        poseStack.last().pose(),
                        instance.animation.pose.globalMatrices[nodeIndex],
                        primitive.bounds,
                        camera
                    )
                ) {
                    continue
                }
                if (renderer.transparent() && !instance.lodSelector.transparent(distanceSquared)) continue
                if (gpuEnabled && gpuCompatible(instance, nodeIndex, primitive)) {
                    val gpuResources = resource.gpu()
                    if (!gpuResources.failed(meshIndex, primitiveIndex)) {
                        val instanceCount = node.instanceMatrices.size / 16
                        if (instanceCount > 0) {
                            for (instanceIndex in 0 until instanceCount) {
                                instanceMatrix.set(node.instanceMatrices, instanceIndex * 16)
                                val submit = gpuSubmits[primitiveIndex][instanceIndex]
                                submit.configure(
                                    resource,
                                    textures,
                                    light,
                                    overlay,
                                    poseStack.last().pose(),
                                    instanceMatrix
                                )
                                submitInstance(submitNodeCollector, renderer, submit)
                            }
                        } else {
                            val submit = gpuSubmits[primitiveIndex][0]
                            submit.configure(resource, textures, light, overlay, poseStack.last().pose())
                            submitInstance(submitNodeCollector, renderer, submit)
                        }
                        continue
                    }
                }
                renderer.light = light
                renderer.overlay = overlay
                poseStack.pushPose()
                if (node.skinIndex < 0) poseStack.mulPose(instance.animation.pose.globalMatrices[nodeIndex])
                val instanceCount = node.instanceMatrices.size / 16
                if (instanceCount > 0) {
                    for (instanceIndex in 0 until instanceCount) {
                        poseStack.pushPose()
                        poseStack.mulPose(instanceMatrix.set(node.instanceMatrices, instanceIndex * 16))
                        submitNodeCollector.submitCustomGeometry(poseStack, renderer.renderType(resource, textures), renderer)
                        poseStack.popPose()
                    }
                } else {
                    submitNodeCollector.submitCustomGeometry(poseStack, renderer.renderType(resource, textures), renderer)
                }
                poseStack.popPose()
            }
        }
        poseStack.popPose()
    }

    private fun nodeVisible(
        relativePose: Matrix4fc,
        nodeMatrix: Matrix4fc,
        bounds: FloatArray,
        camera: CameraRenderState
    ): Boolean {
        cullMatrix.set(relativePose).mul(nodeMatrix)
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (x in 0..1) {
            for (y in 0..1) {
                for (z in 0..1) {
                    cullPoint.set(bounds[x * 3], bounds[y * 3 + 1], bounds[z * 3 + 2])
                    cullMatrix.transformPosition(cullPoint)
                    minX = minOf(minX, cullPoint.x)
                    minY = minOf(minY, cullPoint.y)
                    minZ = minOf(minZ, cullPoint.z)
                    maxX = maxOf(maxX, cullPoint.x)
                    maxY = maxOf(maxY, cullPoint.y)
                    maxZ = maxOf(maxZ, cullPoint.z)
                }
            }
        }
        return (camera.cullFrustum as FrustumAccessor).`libgltf$intersection`()
            .testAab(minX, minY, minZ, maxX, maxY, maxZ)
    }

    private fun submitInstance(
        submitNodeCollector: OrderedSubmitNodeCollector,
        renderer: GltfGeometryRenderer,
        submit: GpuSubmit
    ) {
        if (renderer.transparent()) {
            submitNodeCollector.submitCustom(SubmitRenderPhases.TRANSLUCENT_MODELS, submit)
        } else {
            submitNodeCollector.submitCustom(SubmitRenderPhases.SOLID, submit)
        }
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
        val resource = GltfRenderSystem.resource(instance.handle.resourceId) ?: return
        val textures = resource.textures()
        val asset = instance.handle.asset
        poseStack.pushPose()
        poseStack.mulPose(transform)
        for (nodeIndex in asset.topologicalOrder) {
            if (!instance.sceneMask[nodeIndex]) continue
            val node = asset.nodes[nodeIndex]
            val effectiveMeshIndex = if (node.meshLods.isNotEmpty()) node.meshLods[0] else node.meshIndex
            if (effectiveMeshIndex < 0) continue
            val renderers = instance.geometryRenderers[nodeIndex][0]
            val mesh = asset.meshes[effectiveMeshIndex]
            for (primitiveIndex in mesh.primitives.indices) {
                val primitive = mesh.primitives[primitiveIndex]
                val renderer = renderers[primitiveIndex]
                renderer.light = light
                renderer.overlay = overlay
                poseStack.pushPose()
                if (node.skinIndex < 0) poseStack.mulPose(instance.animation.pose.globalMatrices[nodeIndex])
                val sourceMaterialIndex = primitive.materialIndex.coerceIn(0, asset.materials.lastIndex)
                val materialIndex = instance.resolvePrimitiveMaterial(sourceMaterialIndex, primitive.materialMappings)
                val material = asset.materials[materialIndex]
                val override = instance.materialOverrides[sourceMaterialIndex]
                val texture = when {
                    override?.baseColorIdentifier != null -> override.baseColorIdentifier
                    override?.baseColorTextureIndex != null && override.baseColorTextureIndex >= 0 ->
                        textures.identifier(override.baseColorTextureIndex)
                    material.baseColorTexture != null -> textures.identifier(material.baseColorTexture.textureIndex)
                    else -> textures.materialIdentifier(materialIndex)
                }
                val instanceCount = node.instanceMatrices.size / 16
                if (instanceCount > 0) {
                    for (instanceIndex in 0 until instanceCount) {
                        poseStack.pushPose()
                        poseStack.mulPose(instanceMatrix.set(node.instanceMatrices, instanceIndex * 16))
                        submitNodeCollector.submitCustomGeometry(poseStack, GltfRenderTypes.glint(texture), renderer)
                        poseStack.popPose()
                    }
                } else {
                    submitNodeCollector.submitCustomGeometry(poseStack, GltfRenderTypes.glint(texture), renderer)
                }
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
        if (GpuBackend.capabilities().backend == GpuBackendType.OPENGL && primitive.skin != null) return false
        val asset = instance.handle.asset
        val node = asset.nodes[nodeIndex]
        if (primitive.skin != null && node.skinIndex < 0) return false
        val sourceMaterialIndex = primitive.materialIndex.coerceIn(0, asset.materials.lastIndex)
        val material = asset.materials[instance.resolvePrimitiveMaterial(sourceMaterialIndex, primitive.materialMappings)]
        if (instance.materialOverrides[sourceMaterialIndex]?.baseColorIdentifier != null) return true
        val overrideTexture = instance.materialOverrides[sourceMaterialIndex]?.baseColorTextureIndex ?: -1
        val textureIndex = if (overrideTexture >= 0) overrideTexture else material.baseColorTexture?.textureIndex ?: -1
        val sampler = asset.textures.getOrNull(textureIndex)?.sampler ?: return true
        return sampler.wrapS != TextureWrap.MIRRORED_REPEAT && sampler.wrapT != TextureWrap.MIRRORED_REPEAT
    }
}
