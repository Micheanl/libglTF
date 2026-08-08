package com.micheanl.libgltf.render.vulkan

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.logging.LogUtils
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.commands.GpuFence
import java.nio.ByteOrder

class GltfVulkanMeshCounters : AutoCloseable {
    private val device = RenderSystem.getDevice()
    private val counters = device.createBuffer(
        { "libgltf mesh debug counters" },
        GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_COPY_DST or GltfVulkanUsage.STORAGE,
        java.nio.ByteBuffer.allocateDirect(COUNTER_SIZE)
    )
    private val readback = device.createBuffer(
        { "libgltf mesh debug readback" },
        GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_COPY_DST,
        COUNTER_SIZE.toLong()
    )
    private var fence: GpuFence? = null
    private val last = IntArray(COUNTER_COUNT)

    fun buffer(): GpuBuffer = counters

    fun read() {
        val previous = fence
        if (previous != null) {
            if (previous.awaitCompletion(0L)) {
                fence = null
                readback.map(true, false).use { view ->
                    val data = view.data().order(ByteOrder.nativeOrder())
                    val current = IntArray(COUNTER_COUNT) { data.getInt(it * Int.SIZE_BYTES) }
                    LOGGER.info(
                        "libgltf Vulkan mesh counters tasks={} emitted={} meshes={} primitives={} culled={} occluded={}",
                        current[0] - last[0],
                        current[1] - last[1],
                        current[2] - last[2],
                        current[3] - last[3],
                        current[5] - last[5],
                        current[4] - last[4]
                    )
                    current.copyInto(last)
                }
                previous.close()
            }
        }
        if (fence == null) {
            val encoder = device.createCommandEncoder()
            encoder.copyToBuffer(counters.slice(), readback.slice())
            fence = encoder.createFence()
        }
    }

    override fun close() {
        fence?.close()
        readback.close()
        counters.close()
    }

    private companion object {
        const val COUNTER_COUNT = 6
        const val COUNTER_SIZE = COUNTER_COUNT * Int.SIZE_BYTES
        val LOGGER = LogUtils.getLogger()
    }
}
