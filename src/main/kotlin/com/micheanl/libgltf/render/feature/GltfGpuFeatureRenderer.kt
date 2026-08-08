package com.micheanl.libgltf.render.feature

import com.micheanl.libgltf.render.vulkan.GltfGpuDrivenBenchmark
import com.micheanl.libgltf.render.vulkan.GltfGpuDrivenSettings
import com.micheanl.libgltf.render.vulkan.GltfVulkanGpuDriven
import com.micheanl.libgltf.render.gl.GltfGlGpuDriven
import com.micheanl.libgltf.render.gpu.GltfGpuDriver
import com.mojang.renderpearl.api.commands.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.logging.LogUtils
import net.minecraft.client.renderer.feature.FeatureFrameContext
import net.minecraft.client.renderer.feature.FeatureRenderer
import net.minecraft.client.renderer.oit.OitStage

class GltfGpuFeatureRenderer : FeatureRenderer<GltfGpuSubmit> {
    private val batches = ArrayList<GltfPreparedGpuBatch>()
    private var groupStarts = IntArray(INITIAL_GROUP_CAPACITY)
    private var groupCounts = IntArray(INITIAL_GROUP_CAPACITY)
    private var preparedBatchCount = 0
    private var preparedGroupCount = 0
    private var frameInstances = 0

    override fun beginPrepare(context: FeatureFrameContext) {
        if (!gpuDrivenAttempted) {
            gpuDriven?.close()
            gpuDriven = null
            gpuDrivenAttempted = true
            try {
                gpuDriven = GltfVulkanGpuDriven.create(RenderSystem.getDevice()) ?: GltfGlGpuDriven.create()
                activeMesh = gpuDriven?.meshSupported == true
            } catch (error: RuntimeException) {
                activeMesh = false
                LOGGER.error("libgltf Vulkan GPU-driven initialization failed", error)
            }
        }
        preparedBatchCount = 0
        preparedGroupCount = 0
        frameInstances = 0
    }

    override fun prepareGroup(context: FeatureFrameContext, submits: List<GltfGpuSubmit>, strictlyOrdered: Boolean) {
        frameInstances += submits.size
        ensureGroupCapacity(preparedGroupCount + 1)
        val start = preparedBatchCount
        if (strictlyOrdered) {
            for (index in submits.indices) prepareBatch(submits, index, index + 1)
        } else {
            prepareBatches(submits)
        }
        groupStarts[preparedGroupCount] = start
        groupCounts[preparedGroupCount] = preparedBatchCount - start
        preparedGroupCount++
    }

    private fun prepareBatches(submits: List<GltfGpuSubmit>) {
        if (submits.isEmpty()) return
        var fromIndex = 0
        var key = submits[0].batchKey()
        for (index in 1 until submits.size) {
            val nextKey = submits[index].batchKey()
            if (nextKey != key) {
                prepareBatch(submits, fromIndex, index)
                fromIndex = index
                key = nextKey
            }
        }
        prepareBatch(submits, fromIndex, submits.size)
    }

    override fun executeGroup(
        context: FeatureFrameContext,
        stage: OitStage?,
        renderPass: RenderPass,
        groupIndex: Int,
        submits: List<GltfGpuSubmit>,
        strictlyOrdered: Boolean
    ) {
        val start = groupStarts[groupIndex]
        val end = start + groupCounts[groupIndex]
        for (index in start until end) batches[index].execute(stage, renderPass)
    }

    override fun finishExecute(context: FeatureFrameContext) {
        lastFrameBatches = preparedBatchCount
        lastFrameInstances = frameInstances
        for (index in 0 until preparedBatchCount) batches[index].finishFrame()
        GltfGpuDrivenBenchmark.resolve()
    }

    override fun close() {
        for (batch in batches) batch.close()
        batches.clear()
        gpuDriven?.close()
        gpuDriven = null
        GltfGpuDrivenBenchmark.close()
    }

    private fun prepareBatch(submits: List<GltfGpuSubmit>, fromIndex: Int, toIndex: Int) {
        val batch = if (preparedBatchCount < batches.size) {
            batches[preparedBatchCount]
        } else {
            GltfPreparedGpuBatch().also(batches::add)
        }
        batch.prepare(submits, fromIndex, toIndex, gpuDriven)
        preparedBatchCount++
    }

    private fun ensureGroupCapacity(required: Int) {
        if (required <= groupStarts.size) return
        val capacity = maxOf(required, groupStarts.size shl 1)
        groupStarts = groupStarts.copyOf(capacity)
        groupCounts = groupCounts.copyOf(capacity)
    }

    companion object {
        const val INITIAL_GROUP_CAPACITY = 16
        @Volatile
        var gpuDriven: GltfGpuDriver? = null

        @Volatile
        var gpuDrivenAttempted = false

        @JvmField
        @Volatile
        var activeMesh: Boolean = false

        @JvmField
        @Volatile
        var lastFrameBatches: Int = 0

        @JvmField
        @Volatile
        var lastFrameInstances: Int = 0
        val LOGGER = LogUtils.getLogger()

        fun setMeshShader(enabled: Boolean) {
            GltfGpuDrivenSettings.meshShaderOverride = enabled
            recreate()
        }

        fun resetMeshShader() {
            GltfGpuDrivenSettings.meshShaderOverride = null
            recreate()
        }

        fun recreate() {
            gpuDrivenAttempted = false
            activeMesh = false
        }
    }
}
