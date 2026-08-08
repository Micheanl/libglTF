package com.micheanl.libgltf.render.feature

import net.minecraft.client.renderer.rendertype.RenderType

data class GpuBatchKey(
    val resourceId: Long,
    val meshIndex: Int,
    val primitiveIndex: Int,
    val lod: Int,
    val skinIndex: Int,
    val renderType: RenderType
)
