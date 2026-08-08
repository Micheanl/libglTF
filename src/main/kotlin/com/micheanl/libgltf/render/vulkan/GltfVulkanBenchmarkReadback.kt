package com.micheanl.libgltf.render.vulkan

import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.commands.GpuFence
import com.mojang.blaze3d.systems.RenderSystem
import java.nio.ByteOrder

class GltfVulkanBenchmarkReadback : AutoCloseable {
    private val buffers = Array(BUFFER_COUNT) { index ->
        RenderSystem.getDevice().createBuffer(
            { "libgltf benchmark stats #$index" },
            GpuBuffer.USAGE_MAP_READ or GpuBuffer.USAGE_COPY_DST,
            GltfVulkanComputePipeline.STATS_SIZE.toLong()
        )
    }
    private val fences = arrayOfNulls<GpuFence>(BUFFER_COUNT)
    private val recordNanos = LongArray(BUFFER_COUNT)
    private val instances = IntArray(BUFFER_COUNT)
    private val meshlets = IntArray(BUFFER_COUNT)
    private val triangles = IntArray(BUFFER_COUNT)
    private val lods = IntArray(BUFFER_COUNT)
    private var current = 0

    fun capture(
        stats: GpuBuffer,
        nanos: Long,
        instanceCount: Int,
        meshletCount: Int,
        triangleCount: Int,
        lod: Int
    ) {
        val fence = fences[current]
        if (fence != null) {
            if (!fence.awaitCompletion(0L)) return
            buffers[current].map(true, false).use { view ->
                val data = view.data().order(ByteOrder.nativeOrder())
                val drawCount = data.getInt(0)
                val visibleInstances = data.getInt(4)
                val visibleMeshlets = data.getInt(8)
                GltfGpuDrivenBenchmark.record(
                    recordNanos[current],
                    instances[current],
                    visibleInstances,
                    instances[current] - visibleInstances,
                    visibleMeshlets,
                    instances[current] * meshlets[current] - visibleMeshlets,
                    drawCount,
                    triangles[current],
                    lods[current],
                    if (meshlets[current] > 1) "meshlet" else "indirect",
                    false,
                    false
                )
            }
            fence.close()
            fences[current] = null
        }
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        encoder.copyToBuffer(stats.slice(), buffers[current].slice())
        fences[current] = encoder.createFence()
        recordNanos[current] = nanos
        instances[current] = instanceCount
        meshlets[current] = meshletCount
        triangles[current] = triangleCount
        lods[current] = lod
        current = (current + 1) % BUFFER_COUNT
    }

    override fun close() {
        for (fence in fences) fence?.close()
        for (buffer in buffers) buffer.close()
    }

    private companion object {
        const val BUFFER_COUNT = 3
    }
}