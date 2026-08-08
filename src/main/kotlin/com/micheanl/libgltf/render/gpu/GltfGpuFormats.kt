package com.micheanl.libgltf.render.gpu

import com.mojang.renderpearl.api.GpuFormat
import com.mojang.renderpearl.api.vertex.VertexFormat

object GltfGpuFormats {
    val GEOMETRY: VertexFormat = VertexFormat.builder(0)
        .addAttribute("Position", GpuFormat.RGB32_FLOAT)
        .addAttribute("Normal", GpuFormat.RGB32_FLOAT)
        .addAttribute("Tangent", GpuFormat.RGBA32_FLOAT)
        .addAttribute("UV0", GpuFormat.RG32_FLOAT)
        .addAttribute("TexCoord1", GpuFormat.RG32_FLOAT)
        .addAttribute("Color", GpuFormat.RGBA8_UNORM)
        .build()

    val SKIN: VertexFormat = VertexFormat.builder(0)
        .addAttribute("Joints", GpuFormat.RGBA16_UINT)
        .addAttribute("Weights", GpuFormat.RGBA16_UNORM)
        .build()

    val INSTANCE: VertexFormat = VertexFormat.builder(1)
        .addAttribute("InstanceMatrix0", GpuFormat.RGBA32_FLOAT)
        .addAttribute("InstanceMatrix1", GpuFormat.RGBA32_FLOAT)
        .addAttribute("InstanceMatrix2", GpuFormat.RGBA32_FLOAT)
        .addAttribute("InstanceMatrix3", GpuFormat.RGBA32_FLOAT)
        .addAttribute("InstanceNormal0", GpuFormat.RGBA32_FLOAT)
        .addAttribute("InstanceNormal1", GpuFormat.RGBA32_FLOAT)
        .addAttribute("InstanceNormal2", GpuFormat.RGBA32_FLOAT)
        .addAttribute("InstanceColor", GpuFormat.RGBA32_FLOAT)
        .addAttribute("InstanceLight", GpuFormat.RG16_SINT)
        .addAttribute("InstanceOverlay", GpuFormat.RG16_SINT)
        .addAttribute("PaletteOffset", GpuFormat.R32_SINT)
        .addAttribute("InstanceUvTransform0", GpuFormat.RGBA32_FLOAT)
        .addAttribute("InstanceUvTransform1", GpuFormat.RG32_FLOAT)
        .build()

    val INSTANCE_STRIDE: Int = INSTANCE.vertexSize

    val REQUIRED_VERTEX_ATTRIBUTES: Int = GEOMETRY.elements.size + INSTANCE.elements.size + SKIN.elements.size
}
