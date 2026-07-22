package com.micheanl.libgltf.render.feature

import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.render.GltfRenderAsset
import com.micheanl.libgltf.render.GltfRenderTypes
import com.micheanl.libgltf.render.texture.GltfTextureSet
import net.minecraft.client.renderer.feature.FeatureRendererType
import net.minecraft.client.renderer.feature.submit.BatchableSubmit
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit
import net.minecraft.client.renderer.rendertype.RenderType
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Matrix4fc

class GltfGpuSubmit(
    val instance: GltfInstance,
    val nodeIndex: Int,
    val meshIndex: Int,
    val primitiveIndex: Int
) : BatchableSubmit, TranslucentSubmit {
    val modelMatrix: Matrix4f = Matrix4f()
    val normalMatrix: Matrix3f = Matrix3f()
    var red: Float = 1.0f
        private set
    var green: Float = 1.0f
        private set
    var blue: Float = 1.0f
        private set
    var alpha: Float = 1.0f
        private set
    var light: Int = 0
        private set
    var overlay: Int = 0
        private set
    var lod: Int = 0
        private set
    var skinIndex: Int = -1
        private set
    lateinit var resource: GltfRenderAsset
        private set
    lateinit var renderType: RenderType
        private set

    private lateinit var batchKey: GltfGpuBatchKey
    private var cachedResourceId = Long.MIN_VALUE
    private var cachedMaterialRevision = Long.MIN_VALUE
    private var cachedSkinned = false
    private var cachedLod = -1
    private var distanceToCameraSquared = 0.0f

    override fun featureType(): FeatureRendererType<GltfGpuSubmit> = GltfGpuFeature.TYPE

    override fun batchKey(): Any = batchKey

    override fun distanceToCameraSq(): Float = distanceToCameraSquared

    fun configure(
        resource: GltfRenderAsset,
        textures: GltfTextureSet,
        light: Int,
        overlay: Int,
        transform: Matrix4fc
    ) {
        val asset = resource.asset
        val primitive = asset.meshes[meshIndex].primitives[primitiveIndex]
        val node = asset.nodes[nodeIndex]
        val sourceMaterialIndex = primitive.materialIndex.coerceIn(0, asset.materials.lastIndex)
        val materialIndex = instance.resolveMaterial(sourceMaterialIndex)
        val material = asset.materials[materialIndex]
        val override = instance.materialOverrides[sourceMaterialIndex]
        val factor = override?.baseColorFactor ?: material.baseColorFactor
        red = factor.getOrElse(0) { 1.0f }
        green = factor.getOrElse(1) { 1.0f }
        blue = factor.getOrElse(2) { 1.0f }
        alpha = factor.getOrElse(3) { 1.0f }
        skinIndex = node.skinIndex
        val skinned = skinIndex >= 0 && primitive.skin != null

        this.resource = resource
        this.light = light
        this.overlay = overlay
        lod = instance.lodLevel.coerceAtMost(primitive.lodIndices.lastIndex)

        modelMatrix.set(transform)
        if (skinIndex < 0) modelMatrix.mul(instance.animation.pose.globalMatrices[nodeIndex])
        normalMatrix.set(modelMatrix).invert().transpose()
        updateDistance(primitive.bounds)

        val renderStateChanged =
            cachedResourceId != resource.id ||
                cachedMaterialRevision != instance.materialRevision ||
                cachedSkinned != skinned
        if (renderStateChanged) {
            val overrideIdentifier = override?.baseColorIdentifier
            val overrideTextureIndex = override?.baseColorTextureIndex ?: -1
            val textureIndex = when {
                overrideIdentifier != null -> overrideIdentifier.hashCode()
                overrideTextureIndex >= 0 -> overrideTextureIndex
                else -> material.baseColorTexture?.textureIndex ?: -1
            }
            val overrideCutoff = override?.alphaCutoff ?: Float.NaN
            val alphaCutoff = if (overrideCutoff.isNaN()) material.alphaCutoff else overrideCutoff
            val texture = when {
                overrideIdentifier != null -> overrideIdentifier
                overrideTextureIndex >= 0 -> textures.identifier(overrideTextureIndex)
                else -> textures.materialIdentifier(materialIndex)
            }
            renderType = GltfRenderTypes.getGpu(
                resource.id,
                materialIndex,
                textureIndex,
                alphaCutoff,
                primitive.mode,
                material,
                texture,
                skinned
            )
            cachedResourceId = resource.id
            cachedMaterialRevision = instance.materialRevision
            cachedSkinned = skinned
        }

        if (renderStateChanged || cachedLod != lod) {
            batchKey = GltfGpuBatchKey(
                resource.id,
                meshIndex,
                primitiveIndex,
                lod,
                skinIndex,
                renderType
            )
            cachedLod = lod
        }
    }

    private fun updateDistance(bounds: FloatArray) {
        val centerX = (bounds[0] + bounds[3]) * 0.5f
        val centerY = (bounds[1] + bounds[4]) * 0.5f
        val centerZ = (bounds[2] + bounds[5]) * 0.5f
        val x = modelMatrix.m00() * centerX + modelMatrix.m10() * centerY + modelMatrix.m20() * centerZ + modelMatrix.m30()
        val y = modelMatrix.m01() * centerX + modelMatrix.m11() * centerY + modelMatrix.m21() * centerZ + modelMatrix.m31()
        val z = modelMatrix.m02() * centerX + modelMatrix.m12() * centerY + modelMatrix.m22() * centerZ + modelMatrix.m32()
        distanceToCameraSquared = x * x + y * y + z * z
    }
}
