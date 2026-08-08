package com.micheanl.libgltf.render.feature

/**
 * libgltf · GpuBatchKey
 *
 * <pre><code>
 * batchKey = GpuBatchKey(
 * resource.id,
 * meshIndex,
 * primitiveIndex,
 * lod,
 * skinIndex,
 * renderType
 * )
 * </code></pre>
 *
 * 提交分组键
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import net.minecraft.client.renderer.rendertype.RenderType

data class GpuBatchKey(
    val resourceId: Long,
    val meshIndex: Int,
    val primitiveIndex: Int,
    val lod: Int,
    val skinIndex: Int,
    val renderType: RenderType
)
