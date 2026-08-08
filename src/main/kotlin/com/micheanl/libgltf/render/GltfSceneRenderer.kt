package com.micheanl.libgltf.render

import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.api.GltfRenderMode
import com.micheanl.libgltf.material.TextureWrap
import com.micheanl.libgltf.model.GltfPrimitive
import com.micheanl.libgltf.model.PrimitiveMode
import com.micheanl.libgltf.render.gpu.GltfGpuBackend
import com.micheanl.libgltf.render.iris.IrisCompat
import com.micheanl.libgltf.render.debug.GltfBoneDebugRenderer
import com.mojang.blaze3d.vertex.PoseStack
import java.util.function.Consumer
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OrderedSubmitNodeCollector
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.world.phys.AABB
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector3fc

object GltfSceneRenderer {
    @Volatile
    var lastGpuSubmits: Int = 0

    @Volatile
    var lastCpuSubmits: Int = 0

    @Volatile
    var lastCulledPrimitives: Int = 0

    private val cullMatrix = Matrix4f()
    private val cullPoint = Vector3f()

    fun resetFrameCounters() {
        lastGpuSubmits = 0
        lastCpuSubmits = 0
        lastCulledPrimitives = 0
    }

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
        val camera = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState
        val frustumCulling = camera.isFrustumCaptured
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
                    lastCulledPrimitives++
                    continue
                }
                if (renderer.transparent() && !instance.lodSelector.transparent(distanceSquared)) continue
                if (gpuEnabled && gpuCompatible(instance, nodeIndex, primitive)) {
                    val gpuResources = resource.gpu()
                    if (!gpuResources.failed(meshIndex, primitiveIndex)) {
                        val submit = gpuSubmits[primitiveIndex]
                        submit.configure(resource, textures, light, overlay, poseStack.last().pose())
                        lastGpuSubmits++
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
        if (instance.showBones && asset.skins.isNotEmpty()) {
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), GltfBoneDebugRenderer(instance))
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
        val cameraX = camera.pos.x
        val cameraY = camera.pos.y
        val cameraZ = camera.pos.z
        return camera.cullFrustum.isVisible(
            AABB(
                minX + cameraX,
                minY + cameraY,
                minZ + cameraZ,
                maxX + cameraX,
                maxY + cameraY,
                maxZ + cameraZ
            )
        )
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
            val node = asset.nodes[nodeIndex]
            if (node.meshIndex < 0) continue
            val renderers = instance.geometryRenderers[nodeIndex]
            val mesh = asset.meshes[node.meshIndex]
            for (primitiveIndex in mesh.primitives.indices) {
                val primitive = mesh.primitives[primitiveIndex]
                val renderer = renderers[primitiveIndex]
                renderer.light = light
                renderer.overlay = overlay
                lastCpuSubmits++
                poseStack.pushPose()
                if (node.skinIndex < 0) poseStack.mulPose(instance.animation.pose.globalMatrices[nodeIndex])
                val materialIndex = primitive.materialIndex.coerceIn(0, asset.materials.lastIndex)
                val material = asset.materials[instance.resolveMaterial(materialIndex)]
                val override = instance.materialOverrides[materialIndex]
                val texture = when {
                    override?.baseColorIdentifier != null -> override.baseColorIdentifier
                    override?.baseColorTextureIndex != null && override.baseColorTextureIndex >= 0 ->
                        textures.identifier(override.baseColorTextureIndex)
                    material.baseColorTexture != null -> textures.identifier(material.baseColorTexture.textureIndex)
                    else -> textures.materialIdentifier(materialIndex)
                }
                submitNodeCollector.submitCustomGeometry(poseStack, GltfRenderTypes.glint(texture), renderer)
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
