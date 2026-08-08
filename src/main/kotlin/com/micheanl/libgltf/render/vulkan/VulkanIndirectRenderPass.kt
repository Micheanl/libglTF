package com.micheanl.libgltf.render.vulkan

import com.mojang.renderpearl.api.buffers.GpuBufferSlice

interface VulkanIndirectRenderPass {
    fun drawIndexedIndirectCount(commands: GpuBufferSlice, count: GpuBufferSlice, maxDrawCount: Int)
}