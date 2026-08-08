package com.micheanl.libgltf.render.vulkan

import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.device.GpuDevice

class GltfVulkanBufferRing(
    device: GpuDevice,
    label: String,
    usage: Int,
    size: Int
) : AutoCloseable {
    private val buffers = Array(BUFFER_COUNT) { index ->
        device.createBuffer({ "$label #$index" }, usage, size.toLong())
    }
    private var current = 0

    fun buffer(): GpuBuffer = buffers[current]

    fun rotate() {
        current = (current + 1) % BUFFER_COUNT
    }

    override fun close() {
        for (buffer in buffers) buffer.close()
    }

    private companion object {
        const val BUFFER_COUNT = 3
    }
}