package com.micheanl.libgltf.render.cpu

import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.render.GltfRenderAsset
import com.micheanl.libgltf.render.GltfRenderTypes
import com.micheanl.libgltf.material.AlphaMode
import com.micheanl.libgltf.material.TextureWrap
import com.micheanl.libgltf.model.GltfPrimitive
import com.micheanl.libgltf.model.PrimitiveMode
import com.micheanl.libgltf.model.VertexLayout
import com.micheanl.libgltf.render.texture.GltfTextureSet
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.CompactVectorArray
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

private const val DEFORMED_VERTEX_STRIDE: Int = 6
private val EMPTY_DEFORMED_VERTICES: FloatArray = FloatArray(0)
private val EMPTY_DEFORMED_REVISIONS: LongArray = LongArray(0)

/**
 * libgltf · GltfGeometryRenderer
 *
 * ```
 * internal val geometryRenderers: Array<Array<GltfGeometryRenderer>> = createRenderers()
 * ```
 *
 * CPU 渲染器
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GltfGeometryRenderer(
    private val instance: GltfInstance,
    private val nodeIndex: Int,
    private val primitive: GltfPrimitive
) : SubmitNodeCollector.CustomGeometryRenderer {
    var light: Int = 0
    var overlay: Int = OverlayTexture.NO_OVERLAY

    private val asset = instance.handle.asset
    private val sourceMaterialIndex = primitive.materialIndex.coerceIn(0, asset.materials.lastIndex)
    private val deformedVertices = if (primitive.morphTargetCount > 0 || primitive.skin != null) {
        FloatArray(primitive.vertexCount * DEFORMED_VERTEX_STRIDE)
    } else {
        EMPTY_DEFORMED_VERTICES
    }
    private val deformedRevisions = if (deformedVertices.isNotEmpty()) {
        LongArray(primitive.vertexCount) { Long.MIN_VALUE }
    } else {
        EMPTY_DEFORMED_REVISIONS
    }
    private var sortingCenters: CompactVectorArray? = null
    private var cachedResourceId: Long = Long.MIN_VALUE
    private var cachedMaterialRevision: Long = -1L
    private var cachedRenderType: RenderType? = null
    private val staticVertices: FloatArray? = if (primitive.morphTargetCount == 0 && primitive.skin == null) {
        val vertices = primitive.vertices
        FloatArray(primitive.vertexCount * VertexLayout.STRIDE / Float.SIZE_BYTES).also { array ->
            for (index in array.indices) array[index] = vertices.getFloat(index * Float.SIZE_BYTES)
        }
    } else {
        null
    }

    fun transparent(): Boolean =
        asset.materials[instance.resolvePrimitiveMaterial(sourceMaterialIndex, primitive.materialMappings)]
            .alphaMode == AlphaMode.BLEND

    fun renderType(resource: GltfRenderAsset, textures: GltfTextureSet): RenderType {
        val revision = instance.materialRevision
        val cached = cachedRenderType
        if (cached != null && cachedResourceId == resource.id && cachedMaterialRevision == revision) return cached
        val materialIndex = instance.resolvePrimitiveMaterial(sourceMaterialIndex, primitive.materialMappings)
        val material = asset.materials[materialIndex]
        val override = instance.materialOverrides[sourceMaterialIndex]
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
        val renderType = GltfRenderTypes.get(
            resource.id,
            materialIndex,
            textureIndex,
            alphaCutoff,
            primitive.mode,
            material,
            texture
        )
        cachedResourceId = resource.id
        cachedMaterialRevision = revision
        cachedRenderType = renderType
        return renderType
    }

    override fun render(pose: PoseStack.Pose, buffer: VertexConsumer) {
        val node = asset.nodes[nodeIndex]
        val materialIndex = instance.resolvePrimitiveMaterial(sourceMaterialIndex, primitive.materialMappings)
        val material = asset.materials[materialIndex]
        val override = instance.materialOverrides[sourceMaterialIndex]
        val materialFactor = instance.animation.pose.materialFactor
        val factor = override?.baseColorFactor
            ?: if (materialFactor.animated[materialIndex]) materialFactor.baseColorFactor else material.baseColorFactor
        val redFactor = if (factor.isNotEmpty()) factor[0] else 1.0f
        val greenFactor = if (factor.size > 1) factor[1] else 1.0f
        val blueFactor = if (factor.size > 2) factor[2] else 1.0f
        val alphaFactor = if (factor.size > 3) factor[3] else 1.0f
        val binding = material.baseColorTexture
        var animated = false
        var uvOffsetX = 0.0f
        var uvOffsetY = 0.0f
        var uvScaleX = 1.0f
        var uvScaleY = 1.0f
        var uvCosine = 0.0f
        var uvSine = 0.0f
        if (binding != null) {
            val animatedUv = instance.animation.pose.materialUv
            animated = animatedUv.animated[materialIndex]
            uvOffsetX = if (animated) animatedUv.offsetX[materialIndex] else binding.offsetX
            uvOffsetY = if (animated) animatedUv.offsetY[materialIndex] else binding.offsetY
            uvScaleX = if (animated) animatedUv.scaleX[materialIndex] else binding.scaleX
            uvScaleY = if (animated) animatedUv.scaleY[materialIndex] else binding.scaleY
            val rotation = if (animated) animatedUv.rotation[materialIndex] else binding.rotation
            uvCosine = cos(rotation)
            uvSine = sin(rotation)
        }
        val texture = if (binding == null) null else asset.textures.getOrNull(binding.textureIndex)
        val mirroredS = texture?.sampler?.wrapS == TextureWrap.MIRRORED_REPEAT
        val mirroredT = texture?.sampler?.wrapT == TextureWrap.MIRRORED_REPEAT
        val morphWeights = instance.animationState.morphWeights
        val morphWeightOffset = asset.morphOffsets[nodeIndex]
        val palette = if (primitive.skin != null && node.skinIndex >= 0) {
            instance.animationState.jointPalettes[node.skinIndex]
        } else {
            null
        }
        val revision = instance.animationRevision
        val vertices = primitive.vertices
        val lod = instance.lodLevel.coerceAtMost(primitive.lodIndices.lastIndex)
        val indices = primitive.lodIndices[lod]
        val triangleOrder = if (
            material.alphaMode == AlphaMode.BLEND &&
            primitive.mode == PrimitiveMode.TRIANGLES &&
            !Minecraft.getInstance().gameRenderer.useImprovedTransparency()
        ) {
            sortTriangles(pose, indices, revision, morphWeights, morphWeightOffset, palette)
        } else {
            null
        }
        for (drawIndex in indices.indices) {
            val vertex = if (triangleOrder == null) {
                indices[drawIndex]
            } else {
                indices[triangleOrder[drawIndex / 3] * 3 + drawIndex % 3]
            }
            val base = vertex * VertexLayout.STRIDE
            val deformedBase = vertex * DEFORMED_VERTEX_STRIDE
            val px: Float
            val py: Float
            val pz: Float
            val nx: Float
            val ny: Float
            val nz: Float
            if (deformedVertices.isEmpty()) {
                val static = requireNotNull(staticVertices)
                val position = (base + VertexLayout.POSITION) / Float.SIZE_BYTES
                val normal = (base + VertexLayout.NORMAL) / Float.SIZE_BYTES
                px = static[position]
                py = static[position + 1]
                pz = static[position + 2]
                nx = static[normal]
                ny = static[normal + 1]
                nz = static[normal + 2]
            } else {
                if (deformedRevisions[vertex] != revision) {
                    updateDeformedVertex(vertex, revision, morphWeights, morphWeightOffset, palette)
                }
                px = deformedVertices[deformedBase]
                py = deformedVertices[deformedBase + 1]
                pz = deformedVertices[deformedBase + 2]
                nx = deformedVertices[deformedBase + 3]
                ny = deformedVertices[deformedBase + 4]
                nz = deformedVertices[deformedBase + 5]
            }
            val color = vertices.getInt(base + VertexLayout.COLOR)
            val red = ((color and 0xFF) * redFactor).toInt().coerceIn(0, 255)
            val green = (((color ushr 8) and 0xFF) * greenFactor).toInt().coerceIn(0, 255)
            val blue = (((color ushr 16) and 0xFF) * blueFactor).toInt().coerceIn(0, 255)
            val alpha = (((color ushr 24) and 0xFF) * alphaFactor).toInt().coerceIn(0, 255)
            val uvOffset = base + if (binding?.texCoord == 1) VertexLayout.UV1 else VertexLayout.UV0
            var u = vertices.getFloat(uvOffset)
            var v = vertices.getFloat(uvOffset + 4)
            if (binding != null) {
                val scaledU = u * uvScaleX
                val scaledV = v * uvScaleY
                u = scaledU * uvCosine - scaledV * uvSine + uvOffsetX
                v = scaledU * uvSine + scaledV * uvCosine + uvOffsetY
                if (mirroredS) u = mirrored(u)
                if (mirroredT) v = mirrored(v)
            }
            buffer.addVertex(pose, px, py, pz)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz)
        }
    }

    private fun sortTriangles(
        pose: PoseStack.Pose,
        indices: IntArray,
        revision: Long,
        morphWeights: FloatArray,
        morphWeightOffset: Int,
        palette: FloatArray?
    ): IntArray {
        val triangleCount = indices.size / 3
        var centers = sortingCenters
        if (centers == null || centers.size() != triangleCount) {
            centers = CompactVectorArray(triangleCount)
            sortingCenters = centers
        }
        val vertices = primitive.vertices
        val matrix = pose.pose()
        for (triangle in 0 until triangleCount) {
            val offset = triangle * 3
            var centerX = 0.0f
            var centerY = 0.0f
            var centerZ = 0.0f
            for (corner in 0 until 3) {
                val vertex = indices[offset + corner]
                if (deformedVertices.isNotEmpty() && deformedRevisions[vertex] != revision) {
                    updateDeformedVertex(vertex, revision, morphWeights, morphWeightOffset, palette)
                }
                if (deformedVertices.isEmpty()) {
                    val static = requireNotNull(staticVertices)
                    val vertexOffset = (vertex * VertexLayout.STRIDE + VertexLayout.POSITION) / Float.SIZE_BYTES
                    centerX += static[vertexOffset]
                    centerY += static[vertexOffset + 1]
                    centerZ += static[vertexOffset + 2]
                } else {
                    val vertexOffset = vertex * DEFORMED_VERTEX_STRIDE
                    centerX += deformedVertices[vertexOffset]
                    centerY += deformedVertices[vertexOffset + 1]
                    centerZ += deformedVertices[vertexOffset + 2]
                }
            }
            centerX /= 3.0f
            centerY /= 3.0f
            centerZ /= 3.0f
            val x = matrix.m00() * centerX + matrix.m10() * centerY + matrix.m20() * centerZ + matrix.m30()
            val y = matrix.m01() * centerX + matrix.m11() * centerY + matrix.m21() * centerZ + matrix.m31()
            val z = matrix.m02() * centerX + matrix.m12() * centerY + matrix.m22() * centerZ + matrix.m32()
            centers.set(triangle, x, y, z)
        }
        return RenderSystem.getProjectionType().vertexSorting().sort(centers)
    }

    private fun updateDeformedVertex(
        vertex: Int,
        revision: Long,
        morphWeights: FloatArray,
        morphWeightOffset: Int,
        palette: FloatArray?
    ) {
        val vertices = primitive.vertices
        val base = vertex * VertexLayout.STRIDE
        var px = vertices.getFloat(base + VertexLayout.POSITION)
        var py = vertices.getFloat(base + VertexLayout.POSITION + 4)
        var pz = vertices.getFloat(base + VertexLayout.POSITION + 8)
        var nx = vertices.getFloat(base + VertexLayout.NORMAL)
        var ny = vertices.getFloat(base + VertexLayout.NORMAL + 4)
        var nz = vertices.getFloat(base + VertexLayout.NORMAL + 8)
        if (primitive.morphTargetCount > 0) {
            val vertexOffset = vertex * 3
            val targetStride = primitive.vertexCount * 3
            for (target in 0 until primitive.morphTargetCount) {
                val weightIndex = morphWeightOffset + target
                val weight = if (weightIndex < morphWeights.size) morphWeights[weightIndex] else 0.0f
                if (weight == 0.0f) continue
                val offset = target * targetStride + vertexOffset
                px += primitive.morphPositions[offset] * weight
                py += primitive.morphPositions[offset + 1] * weight
                pz += primitive.morphPositions[offset + 2] * weight
                nx += primitive.morphNormals[offset] * weight
                ny += primitive.morphNormals[offset + 1] * weight
                nz += primitive.morphNormals[offset + 2] * weight
            }
            if (palette == null) {
                val inverseLength =
                    1.0f / sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1.0e-12f)
                nx *= inverseLength
                ny *= inverseLength
                nz *= inverseLength
            }
        }
        val skin = primitive.skin
        if (skin != null && palette != null) {
            val sourceX = px
            val sourceY = py
            val sourceZ = pz
            val sourceNormalX = nx
            val sourceNormalY = ny
            val sourceNormalZ = nz
            val skinBase = vertex * VertexLayout.SKIN_STRIDE
            var positionX = 0.0f
            var positionY = 0.0f
            var positionZ = 0.0f
            var normalX = 0.0f
            var normalY = 0.0f
            var normalZ = 0.0f
            var total = 0.0f
            for (component in 0 until 4) {
                val weight =
                    (skin.getShort(skinBase + VertexLayout.WEIGHTS + component * 2).toInt() and 0xFFFF) / 65535.0f
                if (weight <= 0.0f) continue
                val jointIndex =
                    skin.getShort(skinBase + VertexLayout.JOINTS + component * 2).toInt() and 0xFFFF
                val offset = jointIndex * 16
                if (offset + 15 >= palette.size) continue
                positionX +=
                    (palette[offset] * sourceX +
                        palette[offset + 4] * sourceY +
                        palette[offset + 8] * sourceZ +
                        palette[offset + 12]) * weight
                positionY +=
                    (palette[offset + 1] * sourceX +
                        palette[offset + 5] * sourceY +
                        palette[offset + 9] * sourceZ +
                        palette[offset + 13]) * weight
                positionZ +=
                    (palette[offset + 2] * sourceX +
                        palette[offset + 6] * sourceY +
                        palette[offset + 10] * sourceZ +
                        palette[offset + 14]) * weight
                normalX +=
                    (palette[offset] * sourceNormalX +
                        palette[offset + 4] * sourceNormalY +
                        palette[offset + 8] * sourceNormalZ) * weight
                normalY +=
                    (palette[offset + 1] * sourceNormalX +
                        palette[offset + 5] * sourceNormalY +
                        palette[offset + 9] * sourceNormalZ) * weight
                normalZ +=
                    (palette[offset + 2] * sourceNormalX +
                        palette[offset + 6] * sourceNormalY +
                        palette[offset + 10] * sourceNormalZ) * weight
                total += weight
            }
            if (total > 0.0f) {
                px = positionX
                py = positionY
                pz = positionZ
                val inverseLength =
                    1.0f / sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ).coerceAtLeast(1.0e-12f)
                nx = normalX * inverseLength
                ny = normalY * inverseLength
                nz = normalZ * inverseLength
            }
        }
        val output = vertex * DEFORMED_VERTEX_STRIDE
        deformedVertices[output] = px
        deformedVertices[output + 1] = py
        deformedVertices[output + 2] = pz
        deformedVertices[output + 3] = nx
        deformedVertices[output + 4] = ny
        deformedVertices[output + 5] = nz
        deformedRevisions[vertex] = revision
    }

    private fun mirrored(value: Float): Float {
        val repeated = value - floor(value / 2.0f) * 2.0f
        return if (repeated <= 1.0f) repeated else 2.0f - repeated
    }
}
