package com.micheanl.libgltf.render.vulkan

import com.micheanl.libgltf.render.gpu.GltfMeshletLod
import com.micheanl.libgltf.render.gpu.GltfGpuBackend
import com.mojang.logging.LogUtils
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.pipeline.BlendFunction
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.ShaderSource
import com.mojang.renderpearl.api.pipeline.ShaderType
import com.mojang.renderpearl.api.pipeline.UniformType
import com.mojang.renderpearl.backend.vulkan.VulkanConst
import com.mojang.renderpearl.backend.vulkan.VulkanDevice
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline
import com.mojang.renderpearl.frontend.shaders.GlslCompiler
import it.unimi.dsi.fastutil.longs.LongArrayList
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderDefines
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.shaderc.Shaderc
import org.lwjgl.vulkan.*
import java.util.Collections
import java.util.IdentityHashMap

class GltfVulkanMeshPipelineCache(private val device: VulkanDevice) : AutoCloseable {
    private val pipelines = Collections.synchronizedMap(IdentityHashMap<RenderPipeline, GltfVulkanMeshPipeline>())
    private val failed = Collections.newSetFromMap(IdentityHashMap<RenderPipeline, Boolean>())
    private val maxDrawCount: Int
    private val maxPushDescriptors: Int
    val supported: Boolean

    init {
        MemoryStack.stackPush().use { stack ->
            val mesh = VkPhysicalDeviceMeshShaderPropertiesEXT.calloc(stack).`sType$Default`()
            val push = VkPhysicalDevicePushDescriptorPropertiesKHR.calloc(stack).`sType$Default`()
            mesh.pNext(push.address())
            val root = VkPhysicalDeviceProperties2.calloc(stack).`sType$Default`().pNext(mesh)
            VK12.vkGetPhysicalDeviceProperties2(device.vkDevice().physicalDevice, root)
            maxDrawCount = minOf(mesh.maxTaskWorkGroupCount(0), GltfGpuBackend.vendorProfile().maxTaskGroupCount)
            maxPushDescriptors = push.maxPushDescriptors()
            supported = mesh.maxMeshOutputVertices() >= 64 &&
                mesh.maxMeshOutputPrimitives() >= 124 &&
                mesh.maxTaskWorkGroupInvocations() >= 32 &&
                mesh.maxMeshWorkGroupInvocations() >= 64 &&
                mesh.maxTaskPayloadSize() >= 256 &&
                mesh.maxTaskSharedMemorySize() >= Int.SIZE_BYTES &&
                mesh.maxTaskPayloadAndSharedMemorySize() >= 256 + Int.SIZE_BYTES &&
                mesh.maxMeshPayloadAndSharedMemorySize() >= 256 &&
                mesh.maxTaskWorkGroupSize(0) >= 32 &&
                mesh.maxMeshWorkGroupSize(0) >= 64 &&
                mesh.maxMeshWorkGroupCount(0) >= 32 &&
                mesh.maxMeshWorkGroupTotalCount() >= 32 &&
                maxDrawCount > 0
        }
    }

    fun descriptorPipeline(renderPipeline: RenderPipeline, original: VulkanRenderPipeline): VulkanRenderPipeline? {
        if (!supported || failed.contains(renderPipeline)) return null
        return pipeline(renderPipeline, original)?.descriptorPipeline()
    }

    fun draw(
        renderPipeline: RenderPipeline,
        original: VulkanRenderPipeline,
        commandBuffer: VkCommandBuffer,
        hasDepth: Boolean,
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
            GltfVulkanMeshPipeline.create(device, original, renderPipeline, maxPushDescriptors).also { pipelines[renderPipeline] = it }
        } catch (error: RuntimeException) {
            LOGGER.error("libgltf Vulkan mesh pipeline creation failed for {}", renderPipeline.getLocation(), error)
            failed.add(renderPipeline)
            return false
        }
        pipeline.draw(
            commandBuffer,
            hasDepth,
            geometry,
            instances,
            meshlets,
            sphere,
            instanceCount,
            instanceCulling,
            meshletCulling,
            maxDrawCount
        )
        return true
    }

    private fun pipeline(renderPipeline: RenderPipeline, original: VulkanRenderPipeline): GltfVulkanMeshPipeline? =
        pipelines[renderPipeline] ?: try {
            GltfVulkanMeshPipeline.create(device, original, renderPipeline, maxPushDescriptors).also { pipelines[renderPipeline] = it }
        } catch (error: RuntimeException) {
            LOGGER.error("libgltf Vulkan mesh pipeline creation failed for {}", renderPipeline.getLocation(), error)
            failed.add(renderPipeline)
            null
        }

    override fun close() {
        pipelines.values.forEach(GltfVulkanMeshPipeline::close)
        pipelines.clear()
        failed.clear()
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
    }
}

private class GltfVulkanMeshPipeline(
    private val device: VulkanDevice,
    private val descriptorSetLayout: Long,
    private val pipelineLayout: Long,
    private val withDepthPipeline: Long,
    private val withoutDepthPipeline: Long,
    private val taskModule: Long,
    private val meshModule: Long,
    private val fragmentModule: Long,
    private val storageBinding: Int,
    private val descriptorPipeline: VulkanRenderPipeline
) : AutoCloseable {
    fun descriptorPipeline(): VulkanRenderPipeline = descriptorPipeline

    fun draw(
        commandBuffer: VkCommandBuffer,
        hasDepth: Boolean,
        geometry: GpuBuffer,
        instances: GpuBuffer,
        meshlets: GltfMeshletLod,
        sphere: FloatArray,
        instanceCount: Int,
        instanceCulling: Boolean,
        meshletCulling: Boolean,
        maxDrawCount: Int
    ) {
        VK10.vkCmdBindPipeline(
            commandBuffer,
            VK10.VK_PIPELINE_BIND_POINT_GRAPHICS,
            if (hasDepth || withoutDepthPipeline == 0L) withDepthPipeline else withoutDepthPipeline
        )
        MemoryStack.stackPush().use { stack ->
            val buffers = arrayOf(geometry, instances, meshlets.metadataBuffer, meshlets.vertexBuffer, meshlets.triangleBuffer)
            val infos = VkDescriptorBufferInfo.calloc(buffers.size, stack)
            val writes = VkWriteDescriptorSet.calloc(buffers.size, stack)
            for (index in buffers.indices) {
                infos[index].buffer((buffers[index] as VulkanGpuBuffer).vkBuffer()).offset(0L).range(buffers[index].size())
                writes[index].`sType$Default`().dstBinding(storageBinding + index).descriptorCount(1)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .pBufferInfo(VkDescriptorBufferInfo.create(infos[index].address(), 1))
            }
            KHRPushDescriptor.vkCmdPushDescriptorSetKHR(
                commandBuffer,
                VK10.VK_PIPELINE_BIND_POINT_GRAPHICS,
                pipelineLayout,
                0,
                writes
            )
            val candidateCount = instanceCount.toLong() * meshlets.meshletCount
            var baseCandidate = 0L
            while (baseCandidate < candidateCount) {
                val groups = minOf(
                    maxDrawCount.toLong(),
                    (candidateCount - baseCandidate + TASK_WORKGROUP - 1) / TASK_WORKGROUP
                ).toInt()
                val parameters = stack.malloc(PUSH_CONSTANT_SIZE)
                for (index in sphere.indices) parameters.putFloat(index * Float.SIZE_BYTES, sphere[index])
                parameters.putInt(16, instanceCount)
                parameters.putInt(20, meshlets.meshletCount)
                parameters.putInt(24, if (instanceCulling) 1 else 0)
                parameters.putInt(28, if (meshletCulling) 1 else 0)
                parameters.putInt(32, baseCandidate.toInt())
                parameters.position(0).limit(PUSH_CONSTANT_SIZE)
                VK10.vkCmdPushConstants(
                    commandBuffer,
                    pipelineLayout,
                    EXTMeshShader.VK_SHADER_STAGE_TASK_BIT_EXT,
                    0,
                    parameters
                )
                EXTMeshShader.vkCmdDrawMeshTasksEXT(commandBuffer, groups, 1, 1)
                baseCandidate += groups.toLong() * TASK_WORKGROUP
            }
        }
    }

    override fun close() {
        VK10.vkDestroyPipeline(device.vkDevice(), withoutDepthPipeline, null)
        VK10.vkDestroyPipeline(device.vkDevice(), withDepthPipeline, null)
        VK10.vkDestroyPipelineLayout(device.vkDevice(), pipelineLayout, null)
        VK10.vkDestroyDescriptorSetLayout(device.vkDevice(), descriptorSetLayout, null)
        VK10.vkDestroyShaderModule(device.vkDevice(), fragmentModule, null)
        VK10.vkDestroyShaderModule(device.vkDevice(), meshModule, null)
        VK10.vkDestroyShaderModule(device.vkDevice(), taskModule, null)
    }

    companion object {
        fun create(
            device: VulkanDevice,
            original: VulkanRenderPipeline,
            renderPipeline: RenderPipeline,
            maxPushDescriptors: Int
        ): GltfVulkanMeshPipeline {
            val uniforms = original.uniforms()
            val storageBinding = uniforms.size
            require(storageBinding + DESCRIPTOR_COUNT <= maxPushDescriptors)
            val sampler0Binding = uniforms.indexOfFirst { it.name() == "Sampler0" }
            require(sampler0Binding >= 0)
            val bindings = mapOf(
                "PROJECTION_BINDING" to uniforms.indexOfFirst { it.name() == "Projection" },
                "DYNAMIC_TRANSFORMS_BINDING" to uniforms.indexOfFirst { it.name() == "DynamicTransforms" },
                "LIGHTING_BINDING" to uniforms.indexOfFirst { it.name() == "Lighting" },
                "SAMPLER1_BINDING" to uniforms.indexOfFirst { it.name() == "Sampler1" },
                "SAMPLER2_BINDING" to uniforms.indexOfFirst { it.name() == "Sampler2" },
                "GEOMETRY_BINDING" to storageBinding,
                "INSTANCES_BINDING" to storageBinding + 1,
                "MESHLETS_BINDING" to storageBinding + 2,
                "MESHLET_VERTICES_BINDING" to storageBinding + 3,
                "MESHLET_TRIANGLES_BINDING" to storageBinding + 4
            )
            require(bindings.values.none { it < 0 })
            val taskModule = compileModule(device, TASK_SHADER, Shaderc.shaderc_task_shader, bindings)
            try {
                val meshModule = compileModule(device, MESH_SHADER, Shaderc.shaderc_mesh_shader, bindings)
                try {
                    val fragmentModule = compileFragment(device, renderPipeline, sampler0Binding)
                    try {
                        return create(device, original, renderPipeline, taskModule, meshModule, fragmentModule)
                    } catch (error: RuntimeException) {
                        VK10.vkDestroyShaderModule(device.vkDevice(), fragmentModule, null)
                        throw error
                    }
                } catch (error: RuntimeException) {
                    VK10.vkDestroyShaderModule(device.vkDevice(), meshModule, null)
                    throw error
                }
            } catch (error: RuntimeException) {
                VK10.vkDestroyShaderModule(device.vkDevice(), taskModule, null)
                throw error
            }
        }

        private fun create(
            device: VulkanDevice,
            original: VulkanRenderPipeline,
            renderPipeline: RenderPipeline,
            taskModule: Long,
            meshModule: Long,
            fragmentModule: Long
        ): GltfVulkanMeshPipeline = MemoryStack.stackPush().use { stack ->
            val uniforms = original.uniforms()
            val storageBinding = uniforms.size
            val bindings = VkDescriptorSetLayoutBinding.calloc(storageBinding + DESCRIPTOR_COUNT, stack)
            for (index in uniforms.indices) {
                bindings[index].binding(index).descriptorCount(1)
                    .descriptorType(descriptorType(uniforms[index].type()))
                    .stageFlags(
                        VK10.VK_SHADER_STAGE_VERTEX_BIT or
                            VK10.VK_SHADER_STAGE_FRAGMENT_BIT or
                            EXTMeshShader.VK_SHADER_STAGE_TASK_BIT_EXT or
                            EXTMeshShader.VK_SHADER_STAGE_MESH_BIT_EXT
                    )
            }
            for (index in 0 until DESCRIPTOR_COUNT) {
                bindings[storageBinding + index].binding(storageBinding + index).descriptorCount(1)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .stageFlags(EXTMeshShader.VK_SHADER_STAGE_TASK_BIT_EXT or EXTMeshShader.VK_SHADER_STAGE_MESH_BIT_EXT)
            }
            val descriptorInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).`sType$Default`()
                .flags(KHRPushDescriptor.VK_DESCRIPTOR_SET_LAYOUT_CREATE_PUSH_DESCRIPTOR_BIT_KHR)
                .pBindings(bindings)
            val pointer = stack.mallocLong(1)
            checkVk(VK10.vkCreateDescriptorSetLayout(device.vkDevice(), descriptorInfo, null, pointer))
            val descriptorSetLayout = pointer[0]
            try {
                val range = VkPushConstantRange.calloc(1, stack)
                    .stageFlags(EXTMeshShader.VK_SHADER_STAGE_TASK_BIT_EXT)
                    .offset(0)
                    .size(PUSH_CONSTANT_SIZE)
                val layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack).`sType$Default`()
                    .pSetLayouts(stack.longs(descriptorSetLayout))
                    .pPushConstantRanges(range)
                checkVk(VK10.vkCreatePipelineLayout(device.vkDevice(), layoutInfo, null, pointer))
                val pipelineLayout = pointer[0]
                try {
                    val pipelines = createGraphicsPipelines(
                        device,
                        renderPipeline,
                        taskModule,
                        meshModule,
                        fragmentModule,
                        pipelineLayout,
                        stack
                    )
                    val descriptorPipeline = VulkanRenderPipeline(
                        device,
                        original.withDepthPipeline(),
                        original.withoutDepthPipeline(),
                        pipelineLayout,
                        descriptorSetLayout,
                        LongArrayList(),
                        uniforms
                    )
                    GltfVulkanMeshPipeline(
                        device,
                        descriptorSetLayout,
                        pipelineLayout,
                        pipelines[0],
                        pipelines[1],
                        taskModule,
                        meshModule,
                        fragmentModule,
                        storageBinding,
                        descriptorPipeline
                    )
                } catch (error: RuntimeException) {
                    VK10.vkDestroyPipelineLayout(device.vkDevice(), pipelineLayout, null)
                    throw error
                }
            } catch (error: RuntimeException) {
                VK10.vkDestroyDescriptorSetLayout(device.vkDevice(), descriptorSetLayout, null)
                throw error
            }
        }

        private fun createGraphicsPipelines(
            device: VulkanDevice,
            renderPipeline: RenderPipeline,
            taskModule: Long,
            meshModule: Long,
            fragmentModule: Long,
            pipelineLayout: Long,
            stack: MemoryStack
        ): LongArray {
            val polygonMode = renderPipeline.getPolygonMode()
            val cull = renderPipeline.isCull()
            val depthStencilState = renderPipeline.getDepthStencilState()
            val stages = VkPipelineShaderStageCreateInfo.calloc(3, stack)
            val main = stack.UTF8("main")
            stages[0].`sType$Default`().stage(EXTMeshShader.VK_SHADER_STAGE_TASK_BIT_EXT).module(taskModule).pName(main)
            stages[1].`sType$Default`().stage(EXTMeshShader.VK_SHADER_STAGE_MESH_BIT_EXT).module(meshModule).pName(main)
            stages[2].`sType$Default`().stage(VK10.VK_SHADER_STAGE_FRAGMENT_BIT).module(fragmentModule).pName(main)
            val rasterization = VkPipelineRasterizationStateCreateInfo.calloc(stack).`sType$Default`()
                .polygonMode(VulkanConst.toVk(polygonMode))
                .cullMode(if (cull) VK10.VK_CULL_MODE_BACK_BIT else VK10.VK_CULL_MODE_NONE)
                .frontFace(VK10.VK_FRONT_FACE_CLOCKWISE)
                .lineWidth(1.0f)
            val depth = VkPipelineDepthStencilStateCreateInfo.calloc(stack).`sType$Default`()
            depthStencilState?.let {
                rasterization.depthBiasEnable(it.depthBiasConstant() != 0.0f && it.depthBiasScaleFactor() != 0.0f)
                    .depthBiasConstantFactor(it.depthBiasConstant())
                    .depthBiasSlopeFactor(it.depthBiasScaleFactor())
                depth.depthTestEnable(true)
                    .depthWriteEnable(it.writeDepth())
                    .depthCompareOp(VulkanConst.toVk(it.depthTest()))
            }
            val targets = renderPipeline.getColorTargetStates()
            val attachments = VkPipelineColorBlendAttachmentState.calloc(targets.size, stack)
            for (target in targets) {
                attachments.colorWriteMask(if (target == null) 0 else VulkanConst.toVk(target))
                target?.blendFunction()?.ifPresent { applyBlend(attachments, it) }
                attachments.position(attachments.position() + 1)
            }
            attachments.position(0)
            val rendering = VkPipelineRenderingCreateInfoKHR.calloc(stack).`sType$Default`()
            val formats = stack.mallocInt(targets.size)
            for (index in targets.indices) formats.put(index, targets[index]?.let { VulkanConst.toVk(it.format()) } ?: 0)
            rendering.pColorAttachmentFormats(formats).depthAttachmentFormat(VK10.VK_FORMAT_D32_SFLOAT)
            val createInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack).`sType$Default`()
                .pStages(stages)
                .pRasterizationState(rasterization)
                .pDepthStencilState(depth)
                .pColorBlendState(VkPipelineColorBlendStateCreateInfo.calloc(stack).`sType$Default`().pAttachments(attachments))
                .pViewportState(VkPipelineViewportStateCreateInfo.calloc(stack).`sType$Default`().scissorCount(1).viewportCount(1))
                .pMultisampleState(
                    VkPipelineMultisampleStateCreateInfo.calloc(stack).`sType$Default`()
                        .rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT)
                )
                .pDynamicState(
                    VkPipelineDynamicStateCreateInfo.calloc(stack).`sType$Default`()
                        .pDynamicStates(stack.ints(VK10.VK_DYNAMIC_STATE_VIEWPORT, VK10.VK_DYNAMIC_STATE_SCISSOR))
                )
                .layout(pipelineLayout)
                .pNext(rendering)
            val pointer = stack.mallocLong(1)
            checkVk(VK10.vkCreateGraphicsPipelines(device.vkDevice(), 0L, createInfo, null, pointer))
            val withDepth = pointer[0]
            if (depthStencilState != null) return longArrayOf(withDepth, 0L)
            rendering.depthAttachmentFormat(VK10.VK_FORMAT_UNDEFINED)
            return try {
                checkVk(VK10.vkCreateGraphicsPipelines(device.vkDevice(), 0L, createInfo, null, pointer))
                longArrayOf(withDepth, pointer[0])
            } catch (error: RuntimeException) {
                VK10.vkDestroyPipeline(device.vkDevice(), withDepth, null)
                throw error
            }
        }

        private fun compileModule(
            device: VulkanDevice,
            path: String,
            kind: Int,
            bindings: Map<String, Int>
        ): Long {
            val source = requireNotNull(GltfVulkanMeshPipeline::class.java.getResourceAsStream(path))
                .bufferedReader()
                .use { it.readText() }
            val compiler = Shaderc.shaderc_compiler_initialize()
            val options = Shaderc.shaderc_compile_options_initialize()
            check(compiler != MemoryUtil.NULL && options != MemoryUtil.NULL)
            Shaderc.shaderc_compile_options_set_target_env(
                options,
                Shaderc.shaderc_target_env_vulkan,
                Shaderc.shaderc_env_version_vulkan_1_3
            )
            for ((name, value) in bindings) {
                Shaderc.shaderc_compile_options_add_macro_definition(options, name, value.toString())
            }
            return compileSpv(device, compiler, options, source, path, kind)
        }

        private fun compileFragment(
            device: VulkanDevice,
            renderPipeline: RenderPipeline,
            sampler0Binding: Int
        ): Long {
            val shader = requireNotNull(renderPipeline.getShaders()[ShaderType.FRAGMENT])
            val source = requireNotNull(gameShaderSource.get(shader, ShaderType.FRAGMENT))
            val info = device.getDeviceInfo()
            val compiler = GlslCompiler(info.isZZeroToOne(), info.features().shaderDrawParameters())
            try {
                val defines = renderPipeline.getShaderDefines()
                val fragmentDefines = ShaderDefines(
                    defines.values() + ("SAMPLER0_BINDING" to sampler0Binding.toString()),
                    defines.flags()
                )
                val spv = compiler.compileToSpv(
                    shader.toString(),
                    source,
                    ShaderType.FRAGMENT,
                    fragmentDefines,
                    gameShaderSource
                )
                try {
                    MemoryStack.stackPush().use { stack ->
                        val pointer = stack.mallocLong(1)
                        checkVk(
                            VK10.vkCreateShaderModule(
                                device.vkDevice(),
                                VkShaderModuleCreateInfo.calloc(stack).`sType$Default`().pCode(spv.spv()),
                                null,
                                pointer
                            )
                        )
                        return pointer[0]
                    }
                } finally {
                    spv.close()
                }
            } finally {
                compiler.close()
            }
        }

        private val gameShaderSource = ShaderSource { id, type ->
            val location = if (type == null) {
                id.withPrefix("shaders/include/")
            } else {
                type.idConverter().idToFile(id)
            }
            val resource = Minecraft.getInstance().resourceManager.getResource(location).orElse(null)
                ?: return@ShaderSource null
            resource.openAsReader().use { it.readText() }
        }

        private fun compileSpv(
            device: VulkanDevice,
            compiler: Long,
            options: Long,
            source: String,
            path: String,
            kind: Int
        ): Long {
            val result = Shaderc.shaderc_compile_into_spv(compiler, source, kind, path, "main", options)
            try {
                check(result != MemoryUtil.NULL)
                check(Shaderc.shaderc_result_get_compilation_status(result) == Shaderc.shaderc_compilation_status_success) {
                    Shaderc.shaderc_result_get_error_message(result) ?: "Shader compilation failed"
                }
                MemoryStack.stackPush().use { stack ->
                    val pointer = stack.mallocLong(1)
                    checkVk(
                        VK10.vkCreateShaderModule(
                            device.vkDevice(),
                            VkShaderModuleCreateInfo.calloc(stack).`sType$Default`()
                                .pCode(requireNotNull(Shaderc.shaderc_result_get_bytes(result))),
                            null,
                            pointer
                        )
                    )
                    return pointer[0]
                }
            } finally {
                if (result != MemoryUtil.NULL) Shaderc.shaderc_result_release(result)
                Shaderc.shaderc_compile_options_release(options)
                Shaderc.shaderc_compiler_release(compiler)
            }
        }

        private fun applyBlend(state: VkPipelineColorBlendAttachmentState.Buffer, blend: BlendFunction) {
            state.blendEnable(true)
                .colorBlendOp(VulkanConst.toVk(blend.color().op()))
                .alphaBlendOp(VulkanConst.toVk(blend.alpha().op()))
                .dstAlphaBlendFactor(VulkanConst.toVk(blend.alpha().destFactor()))
                .dstColorBlendFactor(VulkanConst.toVk(blend.color().destFactor()))
                .srcAlphaBlendFactor(VulkanConst.toVk(blend.alpha().sourceFactor()))
                .srcColorBlendFactor(VulkanConst.toVk(blend.color().sourceFactor()))
        }

        private fun descriptorType(type: UniformType): Int = when (type) {
            UniformType.UNIFORM_BUFFER -> VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
            UniformType.COMBINED_IMAGE_SAMPLER -> VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
            UniformType.TEXEL_BUFFER -> VK10.VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER
        }

        private fun checkVk(result: Int) = check(result == VK10.VK_SUCCESS) { "Vulkan error $result" }

        private const val TASK_SHADER = "/assets/libgltf/shaders/mesh/gpu_mesh.task"
        private const val MESH_SHADER = "/assets/libgltf/shaders/mesh/gpu_mesh.mesh"
        private const val DESCRIPTOR_COUNT = 5
        private const val TASK_WORKGROUP = 32
        private const val PUSH_CONSTANT_SIZE = 36
    }
}
