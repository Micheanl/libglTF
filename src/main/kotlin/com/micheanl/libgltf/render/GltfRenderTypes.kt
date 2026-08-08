package com.micheanl.libgltf.render

import com.micheanl.libgltf.LibGltf
import com.micheanl.libgltf.material.AlphaMode
import com.micheanl.libgltf.material.GltfMaterial
import com.micheanl.libgltf.model.PrimitiveMode
import com.micheanl.libgltf.render.gpu.GpuFormats
import com.micheanl.libgltf.render.gpu.GpuBackend
import com.micheanl.libgltf.render.GpuBackendType
import com.micheanl.libgltf.render.iris.IrisCompat
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.BindGroupLayout
import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.oit.OitPipelineSet
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.TextureTransform
import net.minecraft.resources.Identifier
import java.util.concurrent.ConcurrentHashMap

private val JOINT_MATRICES_LAYOUT = BindGroupLayout.builder()
    .withUniform("JointMatrices", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT)
    .build()


/**
 * libgltf · GltfRenderTypes
 *
 * ```
 * val renderType = GltfRenderTypes.get(
 * resource.id,
 * materialIndex,
 * textureIndex,
 * alphaCutoff,
 * primitive.mode,
 * material,
 * texture
 * )
 * ```
 *
 * 运行时 RenderType 构建
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object GltfRenderTypes {
    private val resources = ConcurrentHashMap<Long, ConcurrentHashMap<Long, RenderType>>()
    private val gpuResources = ConcurrentHashMap<Long, ConcurrentHashMap<Long, RenderType>>()
    private val glintResources = ConcurrentHashMap<Identifier, RenderType>()

    fun glint(texture: Identifier): RenderType = glintResources.computeIfAbsent(texture) {
        createGlint(it)
    }

    fun get(
        resourceId: Long,
        materialIndex: Int,
        textureIndex: Int,
        alphaCutoff: Float,
        mode: PrimitiveMode,
        material: GltfMaterial,
        texture: Identifier
    ): RenderType {
        val cache = resources.computeIfAbsent(resourceId) { ConcurrentHashMap() }
        val key = key(materialIndex, textureIndex, alphaCutoff, mode, false)
        return cache.computeIfAbsent(key) {
            create(resourceId, materialIndex, textureIndex, alphaCutoff, mode, material, texture)
        }
    }

    fun getGpu(
        resourceId: Long,
        materialIndex: Int,
        textureIndex: Int,
        alphaCutoff: Float,
        mode: PrimitiveMode,
        material: GltfMaterial,
        texture: Identifier,
        skinned: Boolean
    ): RenderType {
        val cache = gpuResources.computeIfAbsent(resourceId) { ConcurrentHashMap() }
        val key = key(materialIndex, textureIndex, alphaCutoff, mode, skinned)
        return cache.computeIfAbsent(key) {
            createGpu(resourceId, materialIndex, textureIndex, alphaCutoff, mode, material, texture, skinned)
        }
    }

    fun remove(resourceId: Long) {
        resources.remove(resourceId)
        gpuResources.remove(resourceId)
        glintResources.clear()
    }

    private fun createGlint(texture: Identifier): RenderType {
        val pipeline = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET, RenderPipelines.GLINT_SNIPPET)
            .withLocation(LibGltf.id("pipeline/glint_${texture.namespace}_${texture.path.replace('/', '_')}"))
            .withVertexShader(LibGltf.id("core/entity"))
            .withFragmentShader(LibGltf.id("core/entity"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .build()
        val setup = RenderSetup.builder(pipeline)
            .withTexture("Sampler0", texture)
            .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
            .useLightmap()
            .useOverlay()
            .createRenderSetup()
        return RenderType.create("libgltf_glint_${texture.namespace}_${texture.path.replace('/', '_')}", setup)
    }

    private fun create(
        resourceId: Long,
        materialIndex: Int,
        textureIndex: Int,
        alphaCutoff: Float,
        mode: PrimitiveMode,
        material: GltfMaterial,
        texture: Identifier
    ): RenderType {
        val suffix = "${resourceId}_${materialIndex}_${textureIndex}_${mode.ordinal}_${alphaCutoff.toBits()}"
        val builder = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(LibGltf.id("pipeline/runtime_$suffix"))
            .withVertexShader(LibGltf.id("core/entity"))
            .withFragmentShader(LibGltf.id("core/entity"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(topology(mode))
            .withCull(!material.doubleSided)
        val oitPipelineSet = if (material.alphaMode == AlphaMode.BLEND) buildOitPipelineSet(suffix, builder) else null
        applyMaterial(builder, material, alphaCutoff)
        val pipeline = builder.build()
        return createRenderType("libgltf_$suffix", pipeline, oitPipelineSet, texture)
    }

    private fun createGpu(
        resourceId: Long,
        materialIndex: Int,
        textureIndex: Int,
        alphaCutoff: Float,
        mode: PrimitiveMode,
        material: GltfMaterial,
        texture: Identifier,
        skinned: Boolean
    ): RenderType {
        val suffix = "gpu_${resourceId}_${materialIndex}_${textureIndex}_${mode.ordinal}_${alphaCutoff.toBits()}_${if (skinned) 1 else 0}"
        val gl = GpuBackend.capabilities().backend == GpuBackendType.OPENGL
        val builder = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(LibGltf.id("pipeline/runtime_$suffix"))
            .withVertexShader(if (gl) LibGltf.id("core/entity_gpu_gl") else LibGltf.id("core/entity_gpu"))
            .withFragmentShader(LibGltf.id("core/entity"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withVertexBinding(0, if (gl) GpuFormats.GEOMETRY_GL else GpuFormats.GEOMETRY)
            .withVertexBinding(1, if (gl) GpuFormats.INSTANCE_GL else GpuFormats.INSTANCE)
            .withPrimitiveTopology(topology(mode))
            .withCull(!material.doubleSided)
        if (skinned && !gl) {
            builder
                .withBindGroupLayout(JOINT_MATRICES_LAYOUT)
                .withVertexBinding(2, GpuFormats.SKIN)
                .withShaderDefine("SKINNED")
        }
        val oitPipelineSet = if (material.alphaMode == AlphaMode.BLEND) buildOitPipelineSet(suffix, builder) else null
        applyMaterial(builder, material, alphaCutoff)
        val pipeline = builder.build()
        return createRenderType("libgltf_$suffix", pipeline, oitPipelineSet, texture)
    }

    private fun createRenderType(
        name: String,
        pipeline: RenderPipeline,
        oitPipelineSet: OitPipelineSet?,
        texture: Identifier
    ): RenderType {
        val builder = RenderSetup.builder(pipeline)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
        if (oitPipelineSet != null) builder.setOitPipelines(oitPipelineSet)
        val setup = builder.createRenderSetup()
        return RenderType.create(name, setup)
    }

    private fun buildOitPipelineSet(suffix: String, builder: RenderPipeline.Builder): OitPipelineSet {
        val base = builder.buildSnippet()
        val depthBounds = RenderPipeline.builder(base, RenderPipelines.OIT_DEPTH_BOUNDS_SNIPPET)
            .withLocation(LibGltf.id("pipeline/oit_depth_bounds_$suffix"))
            .build()
        val transmittance = RenderPipeline.builder(base, RenderPipelines.OIT_TRANSMITTANCE_SNIPPET)
            .withLocation(LibGltf.id("pipeline/oit_transmittance_$suffix"))
            .build()
        val accumulate = RenderPipeline.builder(base, RenderPipelines.OIT_ACCUMULATE_SNIPPET)
            .withLocation(LibGltf.id("pipeline/oit_accumulate_$suffix"))
            .build()
        return OitPipelineSet(depthBounds, transmittance, accumulate)
    }

    private fun applyMaterial(builder: RenderPipeline.Builder, material: GltfMaterial, alphaCutoff: Float) {
        when (material.alphaMode) {
            AlphaMode.MASK -> builder
                .withShaderDefine("ALPHA_CUTOUT", alphaCutoff)
                .withColorTargetState(ColorTargetState.DEFAULT)
            AlphaMode.BLEND -> builder.withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            AlphaMode.OPAQUE -> builder
                .withShaderDefine("ALPHA_OPAQUE")
                .withColorTargetState(ColorTargetState.DEFAULT)
        }
    }

    private fun key(
        materialIndex: Int,
        textureIndex: Int,
        alphaCutoff: Float,
        mode: PrimitiveMode,
        skinned: Boolean
    ): Long {
        val modeMaterial = materialIndex.and(0x0FFF) or
            (mode.ordinal.and(0x7) shl 12) or
            (if (skinned) 1 shl 15 else 0)
        return (modeMaterial.toLong() shl 48) or
            ((textureIndex + 1).toLong().and(0xFFFFL) shl 32) or
            alphaCutoff.toBits().toLong().and(0xFFFFFFFFL)
    }

    private fun topology(mode: PrimitiveMode): PrimitiveTopology = when (mode) {
        PrimitiveMode.TRIANGLES -> PrimitiveTopology.TRIANGLES
        PrimitiveMode.LINES -> PrimitiveTopology.LINES
        PrimitiveMode.POINTS -> PrimitiveTopology.POINTS
    }
}
