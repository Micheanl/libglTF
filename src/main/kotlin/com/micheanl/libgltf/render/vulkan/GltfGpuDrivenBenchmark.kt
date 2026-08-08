package com.micheanl.libgltf.render.vulkan

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.logging.LogUtils
import com.mojang.renderpearl.api.commands.GpuQueryPool
import com.mojang.renderpearl.api.commands.RenderPass
import java.util.ArrayDeque

object GltfGpuDrivenBenchmark {
    private const val QUERY_POOL_SIZE = 4096
    private var queryPool: GpuQueryPool? = null
    private var cursor = 0
    private var timestampPeriod = 1.0f
    private val pending = ArrayDeque<PendingRecord>()

    fun begin(renderPass: RenderPass): Int {
        if (!GltfGpuDrivenSettings.benchmark) return -1
        val pool = queryPool ?: RenderSystem.getDevice().createTimestampQueryPool(QUERY_POOL_SIZE).also {
            queryPool = it
            timestampPeriod = RenderSystem.getDevice().deviceInfo.timestampPeriod()
        }
        if (cursor + 2 > QUERY_POOL_SIZE) {
            resolve()
            cursor = 0
        }
        renderPass.writeTimestamp(pool, cursor)
        return cursor
    }

    fun end(renderPass: RenderPass, start: Int, metadata: RecordMetadata) {
        if (start < 0) return
        renderPass.writeTimestamp(requireNotNull(queryPool), start + 1)
        pending.addLast(PendingRecord(start, metadata))
    }

    fun attachIndirectStats(start: Int, visibleInstances: Int, visibleMeshlets: Int, drawCommandCount: Int) {
        val record = pending.firstOrNull { it.start == start } ?: return
        record.visibleInstances = visibleInstances
        record.visibleMeshlets = visibleMeshlets
        record.drawCommandCount = drawCommandCount
        record.statsReady = true
    }

    fun resolve() {
        val pool = queryPool ?: return
        while (pending.isNotEmpty()) {
            val record = pending.first()
            if (record.metadata.needsStats && !record.statsReady) break
            val start = pool.getValue(record.start)
            val end = pool.getValue(record.start + 1)
            if (start.isEmpty || end.isEmpty) break
            pending.removeFirst()
            val elapsed = ((end.asLong - start.asLong) * timestampPeriod).toLong()
            record(
                elapsed,
                record.metadata.submitCount,
                record.visibleInstances,
                record.metadata.submitCount - record.visibleInstances,
                record.visibleMeshlets,
                record.metadata.submitCount * record.metadata.meshletCount - record.visibleMeshlets,
                record.drawCommandCount,
                record.metadata.triangleCount,
                record.metadata.lod,
                record.metadata.mode,
                record.metadata.skinned,
                record.metadata.transparent,
                record.metadata.taskGroupCount
            )
        }
    }

    fun close() {
        queryPool?.close()
        queryPool = null
        pending.clear()
        cursor = 0
    }

    data class RecordMetadata(
        val mode: String,
        val submitCount: Int,
        val meshletCount: Int,
        val triangleCount: Int,
        val lod: Int,
        val skinned: Boolean,
        val transparent: Boolean,
        val taskGroupCount: Int = 0,
        val needsStats: Boolean = false
    )

    private class PendingRecord(
        val start: Int,
        val metadata: RecordMetadata
    ) {
        var visibleInstances: Int = metadata.submitCount
        var visibleMeshlets: Int = if (metadata.needsStats) 0 else metadata.meshletCount
        var drawCommandCount: Int = if (metadata.needsStats) 0 else 1
        var statsReady: Boolean = !metadata.needsStats
    }

    private var samples = 0
    private var recordNanos = 0L
    private var submits = 0L
    private var visibleInstances = 0L
    private var culledInstances = 0L
    private var visibleMeshlets = 0L
    private var culledMeshlets = 0L
    private var drawCommands = 0L
    private var commandBytes = 0L
    private var taskGroups = 0L
    private var triangles = 0L

    private fun record(
        nanos: Long,
        submitCount: Int,
        visibleInstanceCount: Int,
        culledInstanceCount: Int,
        visibleMeshletCount: Int,
        culledMeshletCount: Int,
        drawCommandCount: Int,
        triangleCount: Int,
        lod: Int,
        mode: String,
        skinned: Boolean,
        transparent: Boolean,
        taskGroupCount: Int = 0
    ) {
        if (!GltfGpuDrivenSettings.benchmark) return
        samples++
        recordNanos += nanos
        submits += submitCount
        visibleInstances += visibleInstanceCount
        culledInstances += culledInstanceCount
        visibleMeshlets += visibleMeshletCount
        culledMeshlets += culledMeshletCount
        drawCommands += drawCommandCount
        commandBytes += drawCommandCount.toLong() * COMMAND_STRIDE
        taskGroups += taskGroupCount
        triangles += triangleCount
        if (samples < REPORT_SAMPLES) return
        LOGGER.info(
            "libgltf benchmark backend=Vulkan mode={} samples={} avgRecordNs={} submits={} visibleInstances={} culledInstances={} visibleMeshlets={} culledMeshlets={} drawCommands={} commandBytes={} taskGroups={} triangles={} lod={} skinned={} transparent={}",
            mode,
            samples,
            recordNanos / samples,
            submits,
            visibleInstances,
            culledInstances,
            visibleMeshlets,
            culledMeshlets,
            drawCommands,
            commandBytes,
            taskGroups,
            triangles,
            lod,
            skinned,
            transparent
        )
        samples = 0
        recordNanos = 0L
        submits = 0L
        visibleInstances = 0L
        culledInstances = 0L
        visibleMeshlets = 0L
        culledMeshlets = 0L
        drawCommands = 0L
        commandBytes = 0L
        taskGroups = 0L
        triangles = 0L
    }

    private const val REPORT_SAMPLES = 300
    private const val COMMAND_STRIDE = 20
    private val LOGGER = LogUtils.getLogger()
}
