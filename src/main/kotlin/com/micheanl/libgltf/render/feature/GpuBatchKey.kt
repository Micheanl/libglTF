package com.micheanl.libgltf.render.feature

import net.minecraft.client.renderer.rendertype.RenderType




/**
 * libgltf · GpuBatchKey
 *
 * ```
 * private lateinit var batchKey: GpuBatchKey
 * ```
 *
 * 提交分组键
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GpuBatchKey(
    val resourceId: Long,
    val meshIndex: Int,
    val primitiveIndex: Int,
    val lod: Int,
    val skinIndex: Int,
    val renderType: RenderType
)
