package com.micheanl.libgltf.render.gl

import com.micheanl.libgltf.render.gpu.GltfMeshletLod
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.logging.LogUtils
import net.minecraft.client.renderer.rendertype.PreparedRenderType
import org.lwjgl.opengl.EXTMeshShader
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33C
import org.lwjgl.opengl.NVMeshShader
import java.util.Collections
import java.util.IdentityHashMap

class GltfGlMeshPipelineCache : AutoCloseable {
    private val pipelines = Collections.synchronizedMap(IdentityHashMap<RenderPipeline, GltfGlMeshPipeline>())
    private val failed = Collections.newSetFromMap(IdentityHashMap<RenderPipeline, Boolean>())
    private val maxDrawCount: Int
    private val useNv: Boolean
    private val meshWorkgroupSize: Int
    val supported: Boolean

    init {
        val caps = GL.getCapabilities()
        val extSupported = caps.GL_EXT_mesh_shader
        val nvSupported = caps.GL_NV_mesh_shader
        useNv = !extSupported && nvSupported
        val outputVerticesTarget = if (useNv) NVMeshShader.GL_MAX_MESH_OUTPUT_VERTICES_NV else EXTMeshShader.GL_MAX_MESH_OUTPUT_VERTICES_EXT
        val outputPrimitivesTarget = if (useNv) NVMeshShader.GL_MAX_MESH_OUTPUT_PRIMITIVES_NV else EXTMeshShader.GL_MAX_MESH_OUTPUT_PRIMITIVES_EXT
        val taskInvocationTarget = if (useNv) NVMeshShader.GL_MAX_TASK_WORK_GROUP_INVOCATIONS_NV else EXTMeshShader.GL_MAX_TASK_WORK_GROUP_INVOCATIONS_EXT
        val meshInvocationTarget = if (useNv) NVMeshShader.GL_MAX_MESH_WORK_GROUP_INVOCATIONS_NV else EXTMeshShader.GL_MAX_MESH_WORK_GROUP_INVOCATIONS_EXT
        val maxMeshInvocations = if (extSupported || nvSupported) query(meshInvocationTarget) else 0
        meshWorkgroupSize = if (maxMeshInvocations >= 64) {
            64
        } else if (maxMeshInvocations >= 32) {
            32
        } else {
            0
        }
        maxDrawCount = if (extSupported || nvSupported) {
            if (useNv) {
                query(NVMeshShader.GL_MAX_DRAW_MESH_TASKS_COUNT_NV)
            } else {
                queryIndexed(EXTMeshShader.GL_MAX_TASK_WORK_GROUP_COUNT_EXT, 0)
            }
        } else {
            0
        }
        supported = (extSupported || nvSupported) &&
            query(outputVerticesTarget) >= 64 &&
            query(outputPrimitivesTarget) >= 124 &&
            query(taskInvocationTarget) >= 32 &&
            meshWorkgroupSize >= 32 &&
            maxDrawCount > 0
        if (!supported) {
            LOGGER.info(
                "libgltf GL mesh limits nv={} ext={} maxMeshOutputVertices={} maxMeshOutputPrimitives={} maxTaskInvocations={} maxMeshInvocations={} meshWorkgroupSize={} maxDrawCount={}",
                nvSupported,
                extSupported,
                query(outputVerticesTarget),
                query(outputPrimitivesTarget),
                query(taskInvocationTarget),
                query(meshInvocationTarget),
                meshWorkgroupSize,
                maxDrawCount
            )
        }
    }

    fun draw(
        renderPipeline: RenderPipeline,
        preparedRenderType: PreparedRenderType,
        geometry: GpuBuffer,
        instances: GpuBuffer,
        meshlets: GltfMeshletLod,
        sphere: FloatArray,
        instanceCount: Int,
        instanceCulling: Boolean,
        meshletCulling: Boolean
    ): Boolean {
        if (!supported || failed.contains(renderPipeline)) return false
        val pipeline = pipelines[renderPipeline] ?: try {
            GltfGlMeshPipeline.create(renderPipeline, useNv, meshWorkgroupSize).also { pipelines[renderPipeline] = it }
        } catch (error: RuntimeException) {
            LOGGER.error("libgltf GL mesh pipeline creation failed for {}", renderPipeline.getLocation(), error)
            failed.add(renderPipeline)
            return false
        }
        return pipeline.draw(
            preparedRenderType,
            geometry,
            instances,
            meshlets,
            sphere,
            instanceCount,
            instanceCulling,
            meshletCulling,
            maxDrawCount
        )
    }

    override fun close() {
        pipelines.values.forEach(GltfGlMeshPipeline::close)
        pipelines.clear()
        failed.clear()
    }

    private fun query(target: Int): Int {
        val buffer = IntArray(1)
        GL33C.glGetIntegerv(target, buffer)
        return buffer[0]
    }

    private fun queryIndexed(target: Int, index: Int): Int {
        val buffer = IntArray(1)
        GL33C.glGetIntegeri_v(target, index, buffer)
        return buffer[0]
    }

    private companion object {
        val LOGGER = LogUtils.getLogger()
    }
}
