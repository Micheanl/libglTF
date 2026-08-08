package com.micheanl.libgltf.render.vulkan

/**
 * libgltf · VulkanBufferRing
 *
 * <pre>{@code
 * commandRing = VulkanBufferRing(
 * device,
 * "libgltf indirect commands",
 * GpuBuffer.USAGE_INDIRECT_PARAMETERS or VulkanUsage.STORAGE,
 * commandCapacity
 * )
 * }</pre>
 *
 * Vulkan 环形缓冲
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.device.GpuDevice

class VulkanBufferRing(
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
