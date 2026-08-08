package com.micheanl.libgltf.render.gl

/**
 * libgltf · GlMeshPipeline
 *
 * <pre>{@code
 * GlMeshPipeline.create(renderPipeline, useNv, meshWorkgroupSize).also { pipelines[renderPipeline] = it }
 * }</pre>
 *
 * OpenGL mesh 管线与绘制
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.mixin.GlBufferAccessor
import com.micheanl.libgltf.mixin.GlSamplerAccessor
import com.micheanl.libgltf.render.gpu.MeshletStorage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuSampler
import com.mojang.renderpearl.api.textures.GpuTextureView
import com.mojang.renderpearl.backend.opengl.GlTexture
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.rendertype.OutputTarget
import net.minecraft.client.renderer.rendertype.PreparedRenderType
import org.lwjgl.opengl.EXTMeshShader
import org.lwjgl.opengl.GL33C
import org.lwjgl.opengl.GL43C
import org.lwjgl.opengl.NVMeshShader
import org.lwjgl.system.MemoryStack

class GlMeshPipeline private constructor(
    private val programId: Int,
    private val paramsUbo: Int,
    private val fallbackUbo: Int,
    private val useNv: Boolean,
    private val transmittance: Boolean,
    private val accumulate: Boolean
) : AutoCloseable {

    fun draw(
        preparedRenderType: PreparedRenderType,
        geometry: GpuBuffer,
        instances: GpuBuffer,
        meshlets: MeshletStorage,
        sphere: FloatArray,
        instanceCount: Int,
        instanceCulling: Boolean,
        meshletCulling: Boolean,
        maxDrawCount: Int
    ): Boolean {
        val projection = RenderSystem.getProjectionMatrixBuffer() ?: return false
        GL33C.glUseProgram(programId)
        bindUbo(BINDING_PROJECTION, projection)
        bindUbo(BINDING_DYNAMIC_TRANSFORMS, preparedRenderType.dynamicTransforms())
        bindUboOrFallback(BINDING_FOG, RenderSystem.getShaderFog())
        bindUboOrFallback(BINDING_LIGHTING, RenderSystem.getShaderLights())
        bindSsbo(BINDING_GEOMETRY, geometry)
        bindSsbo(BINDING_INSTANCES, instances)
        bindSsbo(BINDING_MESHLETS, meshlets.metadataBuffer)
        bindSsbo(BINDING_MESHLET_VERTICES, meshlets.vertexBuffer)
        bindSsbo(BINDING_MESHLET_TRIANGLES, meshlets.triangleBuffer)
        bindTextures(preparedRenderType)
        bindOitSamplers()
        val candidateCount = instanceCount.toLong() * meshlets.meshletCount
        var baseCandidate = 0L
        while (baseCandidate < candidateCount) {
            val groups = minOf(
                maxDrawCount.toLong(),
                (candidateCount - baseCandidate + TASK_WORKGROUP - 1) / TASK_WORKGROUP
            ).toInt()
            writeParams(sphere, instanceCount, meshlets.meshletCount, instanceCulling, meshletCulling, baseCandidate.toInt())
            if (useNv) {
                NVMeshShader.glDrawMeshTasksNV(0, groups)
            } else {
                EXTMeshShader.glDrawMeshTasksEXT(0, groups, 1)
            }
            baseCandidate += groups.toLong() * TASK_WORKGROUP
        }
        GL33C.glUseProgram(0)
        return true
    }

    override fun close() {
        GL33C.glDeleteProgram(programId)
        GL33C.glDeleteBuffers(paramsUbo)
        GL33C.glDeleteBuffers(fallbackUbo)
    }

    private fun bindUbo(binding: Int, slice: GpuBufferSlice) {
        GL33C.glBindBufferRange(
            GL33C.GL_UNIFORM_BUFFER,
            binding,
            (slice.buffer() as GlBufferAccessor).`libgltf$handle`(),
            slice.offset(),
            slice.length()
        )
    }

    private fun bindUboOrFallback(binding: Int, slice: GpuBufferSlice?) {
        if (slice != null) {
            bindUbo(binding, slice)
        } else {
            GL33C.glBindBufferBase(GL33C.GL_UNIFORM_BUFFER, binding, fallbackUbo)
        }
    }

    private fun bindSsbo(binding: Int, buffer: GpuBuffer) {
        GL33C.glBindBufferBase(
            GL43C.GL_SHADER_STORAGE_BUFFER,
            binding,
            (buffer as GlBufferAccessor).`libgltf$handle`()
        )
    }

    private fun bindTextures(preparedRenderType: PreparedRenderType) {
        for (texture in preparedRenderType.textures()) {
            val unit = when (texture.name) {
                "Sampler0" -> 0
                "Sampler1" -> 1
                "Sampler2" -> 2
                else -> continue
            }
            GL33C.glActiveTexture(GL33C.GL_TEXTURE0 + unit)
            GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, (texture.textureView.texture() as GlTexture).glId())
            GL33C.glBindSampler(unit, (texture.sampler as GlSamplerAccessor).`libgltf$getId`())
        }
    }

    private fun bindOitSamplers() {
        if (!transmittance && !accumulate) return
        val nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
        if (transmittance || accumulate) {
            val depthBounds = OutputTarget.DEPTH_BOUNDS_TARGET.getRenderTarget().getColorTextureView()
            if (depthBounds != null) bindSampler(3, depthBounds, nearest)
        }
        if (accumulate) {
            for (index in 0 until LevelRenderer.OIT_TRANSMITTANCE_TARGET_COUNT) {
                val coeff = OutputTarget.TRANSMITTANCE_TARGETS[index].getRenderTarget().getColorTextureView()
                if (coeff != null) bindSampler(4 + index, coeff, nearest)
            }
        }
    }

    private fun bindSampler(unit: Int, view: GpuTextureView, sampler: GpuSampler) {
        GL33C.glActiveTexture(GL33C.GL_TEXTURE0 + unit)
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, (view.texture() as GlTexture).glId())
        GL33C.glBindSampler(unit, (sampler as GlSamplerAccessor).`libgltf$getId`())
    }

    private fun writeParams(
        sphere: FloatArray,
        instanceCount: Int,
        meshletCount: Int,
        instanceCulling: Boolean,
        meshletCulling: Boolean,
        baseCandidate: Int
    ) {
        MemoryStack.stackPush().use { stack ->
            val data = stack.malloc(PARAM_SIZE)
            for (index in sphere.indices) data.putFloat(index * Float.SIZE_BYTES, sphere[index])
            data.putInt(16, instanceCount)
            data.putInt(20, meshletCount)
            data.putInt(24, if (instanceCulling) 1 else 0)
            data.putInt(28, if (meshletCulling) 1 else 0)
            data.putInt(32, baseCandidate)
            data.position(0).limit(PARAM_SIZE)
            GL33C.glBindBuffer(GL33C.GL_UNIFORM_BUFFER, paramsUbo)
            GL33C.glBufferData(GL33C.GL_UNIFORM_BUFFER, data, GL33C.GL_STREAM_DRAW)
            GL33C.glBindBufferBase(GL33C.GL_UNIFORM_BUFFER, BINDING_PARAMS, paramsUbo)
        }
    }

    companion object {
        fun create(renderPipeline: RenderPipeline, useNv: Boolean, meshWorkgroupSize: Int): GlMeshPipeline {
            val taskType = if (useNv) NVMeshShader.GL_TASK_SHADER_NV else EXTMeshShader.GL_TASK_SHADER_EXT
            val meshType = if (useNv) NVMeshShader.GL_MESH_SHADER_NV else EXTMeshShader.GL_MESH_SHADER_EXT
            val meshPath = if (useNv) "/assets/libgltf/shaders/mesh/gpu_mesh_nv.mesh" else "/assets/libgltf/shaders/mesh/gpu_mesh_gl.mesh"
            val taskPath = if (useNv) "/assets/libgltf/shaders/mesh/gpu_mesh_nv.task" else "/assets/libgltf/shaders/mesh/gpu_mesh_gl.task"
            val task = compile(taskType, withConeCulling(shader(taskPath), renderPipeline))
            try {
                val mesh = compile(
                    meshType,
                    withMeshWorkgroupSize(withConeCulling(shader(meshPath), renderPipeline), meshWorkgroupSize)
                )
                try {
                    val defines = renderPipeline.getShaderDefines()
                    val oit = defines.flags().contains("OIT")
                    val fragmentPath = if (oit) {
                        "/assets/libgltf/shaders/core/entity_gl_oit.fsh"
                    } else {
                        "/assets/libgltf/shaders/core/entity_gl.fsh"
                    }
                    val fragmentSource = withDefines(shader(fragmentPath), renderPipeline)
                    val fragment = compile(GL33C.GL_FRAGMENT_SHADER, fragmentSource)
                    try {
                        val program = link(task, mesh, fragment)
                        setup(program)
                        return GlMeshPipeline(
                            program,
                            GL33C.glGenBuffers(),
                            createFallbackUbo(),
                            useNv,
                            oit && defines.flags().contains("OIT_TRANSMITTANCE"),
                            oit && defines.flags().contains("OIT_ACCUMULATE")
                        )
                    } catch (error: RuntimeException) {
                        GL33C.glDeleteShader(fragment)
                        throw error
                    }
                } catch (error: RuntimeException) {
                    GL33C.glDeleteShader(mesh)
                    throw error
                }
            } catch (error: RuntimeException) {
                GL33C.glDeleteShader(task)
                throw error
            }
        }

        private fun shader(path: String): String =
            requireNotNull(GlMeshPipeline::class.java.getResourceAsStream(path))
                .bufferedReader()
                .use { it.readText() }

        private fun withDefines(source: String, renderPipeline: RenderPipeline): String {
            val builder = StringBuilder()
            val defines = renderPipeline.getShaderDefines()
            for ((key, value) in defines.values()) builder.append("#define $key $value\n")
            for (flag in defines.flags()) builder.append("#define $flag\n")
            if (RenderSystem.getDevice().deviceInfo.isZZeroToOne()) {
                builder.append("#define RENDERPEARL_DEPTH_IS_ZERO_TO_ONE\n")
            }
            val versionEnd = source.indexOf('\n') + 1
            return source.substring(0, versionEnd) + builder.toString() + source.substring(versionEnd)
        }

        private fun withMeshWorkgroupSize(source: String, size: Int): String {
            val versionEnd = source.indexOf('\n') + 1
            return source.substring(0, versionEnd) + "#define MESH_WORKGROUP_SIZE $size\n" + source.substring(versionEnd)
        }

        private fun withConeCulling(source: String, renderPipeline: RenderPipeline): String {
            if (!renderPipeline.isCull()) return source
            val versionEnd = source.indexOf('\n') + 1
            return source.substring(0, versionEnd) + "#define MESH_CONE_CULLING\n" + source.substring(versionEnd)
        }

        private fun compile(type: Int, source: String): Int {
            val shader = GL33C.glCreateShader(type)
            GL33C.glShaderSource(shader, source)
            GL33C.glCompileShader(shader)
            check(GL33C.glGetShaderi(shader, GL33C.GL_COMPILE_STATUS) != GL33C.GL_FALSE) {
                GL33C.glGetShaderInfoLog(shader, 4096)
            }
            return shader
        }

        private fun link(task: Int, mesh: Int, fragment: Int): Int {
            val program = GL33C.glCreateProgram()
            GL33C.glAttachShader(program, task)
            GL33C.glAttachShader(program, mesh)
            GL33C.glAttachShader(program, fragment)
            GL33C.glLinkProgram(program)
            check(GL33C.glGetProgrami(program, GL33C.GL_LINK_STATUS) != GL33C.GL_FALSE) {
                GL33C.glGetProgramInfoLog(program, 4096)
            }
            GL33C.glDetachShader(program, task)
            GL33C.glDetachShader(program, mesh)
            GL33C.glDetachShader(program, fragment)
            GL33C.glDeleteShader(task)
            GL33C.glDeleteShader(mesh)
            GL33C.glDeleteShader(fragment)
            return program
        }

        private fun setup(program: Int) {
            GL33C.glUseProgram(program)
            bindBlock(program, "Projection", BINDING_PROJECTION)
            bindBlock(program, "DynamicTransforms", BINDING_DYNAMIC_TRANSFORMS)
            bindBlock(program, "Fog", BINDING_FOG)
            bindBlock(program, "Lighting", BINDING_LIGHTING)
            bindBlock(program, "MeshParams", BINDING_PARAMS)
            bindStorageBlock(program, "Geometry", BINDING_GEOMETRY)
            bindStorageBlock(program, "Instances", BINDING_INSTANCES)
            bindStorageBlock(program, "Meshlets", BINDING_MESHLETS)
            bindStorageBlock(program, "MeshletVertices", BINDING_MESHLET_VERTICES)
            bindStorageBlock(program, "MeshletTriangles", BINDING_MESHLET_TRIANGLES)
            setSampler(program, "Sampler0", 0)
            setSampler(program, "Sampler1", 1)
            setSampler(program, "Sampler2", 2)
            GL33C.glUseProgram(0)
        }

        private fun bindBlock(program: Int, name: String, binding: Int) {
            val index = GL33C.glGetUniformBlockIndex(program, name)
            if (index != GL33C.GL_INVALID_INDEX) GL33C.glUniformBlockBinding(program, index, binding)
        }

        private fun bindStorageBlock(program: Int, name: String, binding: Int) {
            val index = GL43C.glGetProgramResourceIndex(program, GL43C.GL_SHADER_STORAGE_BLOCK, name)
            if (index != GL33C.GL_INVALID_INDEX) {
                GL43C.glShaderStorageBlockBinding(program, index, binding)
            }
        }

        private fun setSampler(program: Int, name: String, unit: Int) {
            val location = GL33C.glGetUniformLocation(program, name)
            if (location >= 0) GL33C.glUniform1i(location, unit)
        }

        private fun createFallbackUbo(): Int {
            val ubo = GL33C.glGenBuffers()
            GL33C.glBindBuffer(GL33C.GL_UNIFORM_BUFFER, ubo)
            GL33C.glBufferData(GL33C.GL_UNIFORM_BUFFER, 64, GL33C.GL_STATIC_DRAW)
            GL33C.glBindBuffer(GL33C.GL_UNIFORM_BUFFER, 0)
            return ubo
        }

        private const val BINDING_PROJECTION = 0
        private const val BINDING_DYNAMIC_TRANSFORMS = 1
        private const val BINDING_FOG = 2
        private const val BINDING_LIGHTING = 3
        private const val BINDING_PARAMS = 5
        private const val BINDING_GEOMETRY = 6
        private const val BINDING_INSTANCES = 7
        private const val BINDING_MESHLETS = 8
        private const val BINDING_MESHLET_VERTICES = 9
        private const val BINDING_MESHLET_TRIANGLES = 10
        private const val PARAM_SIZE = 36
        private const val TASK_WORKGROUP = 1
    }
}
