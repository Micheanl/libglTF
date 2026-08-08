package com.micheanl.libgltf.render.vulkan

/**
 * libgltf · VulkanIndirectRenderPass
 *
 * Vulkan 间接绘制接入
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.mojang.renderpearl.api.buffers.GpuBufferSlice

interface VulkanIndirectRenderPass {
    fun drawIndexedIndirectCount(commands: GpuBufferSlice, count: GpuBufferSlice, maxDrawCount: Int)
}
