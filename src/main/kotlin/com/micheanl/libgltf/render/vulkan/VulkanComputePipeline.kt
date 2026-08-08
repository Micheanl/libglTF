package com.micheanl.libgltf.render.vulkan

import com.micheanl.libgltf.mixin.VulkanCommandEncoderAccessor
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.backend.vulkan.VulkanDevice
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer
import org.joml.Matrix4f
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.shaderc.Shaderc
import org.lwjgl.vulkan.*
import java.nio.ByteBuffer


/**
 * libgltf · VulkanComputePipeline
 *
 * <pre><code>
 * return VulkanGpuDriver(VulkanComputePipeline.create(backend), mesh)
 * </code></pre>
 *
 * 间接路径的计算剔除管线
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class VulkanComputePipeline private constructor(
    private val device: VulkanDevice,
    private val descriptorSetLayout: Long,
    private val pipelineLayout: Long,
    private val pipeline: Long
) : AutoCloseable {
    fun dispatch(
        instances: GpuBuffer,
        metadata: GpuBuffer,
        commands: GpuBuffer,
        stats: GpuBuffer,
        projection: GpuBufferSlice,
        modelView: Matrix4f,
        sphere: FloatArray,
        instanceCount: Int,
        meshletCount: Int,
        instanceCulling: Boolean,
        meshletCulling: Boolean
    ) {
        val commandBuffer = (device.createCommandEncoder() as VulkanCommandEncoderAccessor).`libgltf$commandBuffer`()
        MemoryStack.stackPush().use { stack ->
            VK12.vkCmdFillBuffer(commandBuffer, vulkan(stats), 0L, STATS_SIZE.toLong(), 0)
            val transferBarrier = VkMemoryBarrier.calloc(1, stack).`sType$Default`()
                .srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT or VK10.VK_ACCESS_SHADER_WRITE_BIT)
            VK10.vkCmdPipelineBarrier(
                commandBuffer, VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                0, transferBarrier, null, null
            )
            VK10.vkCmdBindPipeline(commandBuffer, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, pipeline)
            pushDescriptors(stack, commandBuffer, instances, metadata, commands, stats, projection)
            val parameters = stack.malloc(PUSH_CONSTANT_SIZE)
            modelView.get(0, parameters)
            for (index in sphere.indices) parameters.putFloat(64 + index * Float.SIZE_BYTES, sphere[index])
            parameters.putInt(80, instanceCount)
            parameters.putInt(84, meshletCount)
            parameters.putInt(88, if (instanceCulling) 1 else 0)
            parameters.putInt(92, if (meshletCulling) 1 else 0)
            parameters.position(0).limit(PUSH_CONSTANT_SIZE)
            VK10.vkCmdPushConstants(commandBuffer, pipelineLayout, VK10.VK_SHADER_STAGE_COMPUTE_BIT, 0, parameters)
            VK10.vkCmdDispatch(commandBuffer, (meshletCount + WORKGROUP_SIZE - 1) / WORKGROUP_SIZE, instanceCount, 1)
            val computeBarrier = VkMemoryBarrier.calloc(1, stack).`sType$Default`()
                .srcAccessMask(VK10.VK_ACCESS_SHADER_WRITE_BIT)
                .dstAccessMask(VK10.VK_ACCESS_INDIRECT_COMMAND_READ_BIT or VK10.VK_ACCESS_TRANSFER_READ_BIT)
            VK10.vkCmdPipelineBarrier(
                commandBuffer, VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT or VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                0, computeBarrier, null, null
            )
        }
    }

    override fun close() {
        VK10.vkDestroyPipeline(device.vkDevice(), pipeline, null)
        VK10.vkDestroyPipelineLayout(device.vkDevice(), pipelineLayout, null)
        VK10.vkDestroyDescriptorSetLayout(device.vkDevice(), descriptorSetLayout, null)
    }

    private fun pushDescriptors(
        stack: MemoryStack,
        commandBuffer: VkCommandBuffer,
        instances: GpuBuffer,
        metadata: GpuBuffer,
        commands: GpuBuffer,
        stats: GpuBuffer,
        projection: GpuBufferSlice
    ) {
        val infos = VkDescriptorBufferInfo.calloc(DESCRIPTOR_COUNT, stack)
        infos[0].buffer(vulkan(instances)).offset(0L).range(instances.size())
        infos[1].buffer(vulkan(metadata)).offset(0L).range(metadata.size())
        infos[2].buffer(vulkan(commands)).offset(0L).range(commands.size())
        infos[3].buffer(vulkan(stats)).offset(0L).range(stats.size())
        infos[4].buffer(vulkan(projection.buffer())).offset(projection.offset()).range(projection.length())
        val writes = VkWriteDescriptorSet.calloc(DESCRIPTOR_COUNT, stack)
        for (index in 0 until DESCRIPTOR_COUNT) {
            writes[index].`sType$Default`().dstBinding(index).descriptorCount(1)
                .descriptorType(if (index == PROJECTION_BINDING) VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER else VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                .pBufferInfo(VkDescriptorBufferInfo.create(infos[index].address(), 1))
        }
        KHRPushDescriptor.vkCmdPushDescriptorSetKHR(
            commandBuffer, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, pipelineLayout, 0, writes
        )
    }

    private fun vulkan(buffer: GpuBuffer): Long = (buffer as VulkanGpuBuffer).vkBuffer()

    companion object {
        fun create(device: VulkanDevice): VulkanComputePipeline {
            val source = requireNotNull(VulkanComputePipeline::class.java.getResourceAsStream(SHADER_PATH))
                .bufferedReader().use { it.readText() }
            val compiler = Shaderc.shaderc_compiler_initialize()
            check(compiler != MemoryUtil.NULL)
            val result = Shaderc.shaderc_compile_into_spv(
                compiler, source, Shaderc.shaderc_compute_shader, "gpu_cull.comp", "main", MemoryUtil.NULL
            )
            try {
                check(result != MemoryUtil.NULL)
                check(Shaderc.shaderc_result_get_compilation_status(result) == Shaderc.shaderc_compilation_status_success) {
                    Shaderc.shaderc_result_get_error_message(result) ?: "Shader compilation failed"
                }
                return create(device, requireNotNull(Shaderc.shaderc_result_get_bytes(result)))
            } finally {
                if (result != MemoryUtil.NULL) Shaderc.shaderc_result_release(result)
                Shaderc.shaderc_compiler_release(compiler)
            }
        }

        private fun create(device: VulkanDevice, code: ByteBuffer): VulkanComputePipeline {
            MemoryStack.stackPush().use { stack ->
                val bindings = VkDescriptorSetLayoutBinding.calloc(DESCRIPTOR_COUNT, stack)
                for (index in 0 until DESCRIPTOR_COUNT) {
                    bindings[index].binding(index).descriptorCount(1)
                        .descriptorType(if (index == PROJECTION_BINDING) VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER else VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                        .stageFlags(VK10.VK_SHADER_STAGE_COMPUTE_BIT)
                }
                val descriptorInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).`sType$Default`()
                    .flags(KHRPushDescriptor.VK_DESCRIPTOR_SET_LAYOUT_CREATE_PUSH_DESCRIPTOR_BIT_KHR)
                    .pBindings(bindings)
                val descriptorPointer = stack.mallocLong(1)
                checkVk(VK10.vkCreateDescriptorSetLayout(device.vkDevice(), descriptorInfo, null, descriptorPointer))
                val descriptorSetLayout = descriptorPointer[0]
                try {
                    val range = VkPushConstantRange.calloc(1, stack).stageFlags(VK10.VK_SHADER_STAGE_COMPUTE_BIT)
                        .offset(0).size(PUSH_CONSTANT_SIZE)
                    val layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack).`sType$Default`()
                        .pSetLayouts(stack.longs(descriptorSetLayout)).pPushConstantRanges(range)
                    val layoutPointer = stack.mallocLong(1)
                    checkVk(VK10.vkCreatePipelineLayout(device.vkDevice(), layoutInfo, null, layoutPointer))
                    val pipelineLayout = layoutPointer[0]
                    try {
                        val shaderPointer = stack.mallocLong(1)
                        checkVk(
                            VK10.vkCreateShaderModule(
                                device.vkDevice(), VkShaderModuleCreateInfo.calloc(stack).`sType$Default`().pCode(code),
                                null, shaderPointer
                            )
                        )
                        val shaderModule = shaderPointer[0]
                        try {
                            val stage = VkPipelineShaderStageCreateInfo.calloc(stack).`sType$Default`()
                                .stage(VK10.VK_SHADER_STAGE_COMPUTE_BIT).module(shaderModule).pName(stack.UTF8("main"))
                            val pipelineInfo = VkComputePipelineCreateInfo.calloc(1, stack).`sType$Default`()
                                .stage(stage).layout(pipelineLayout)
                            val pipelinePointer = stack.mallocLong(1)
                            checkVk(VK10.vkCreateComputePipelines(device.vkDevice(), 0L, pipelineInfo, null, pipelinePointer))
                            return VulkanComputePipeline(device, descriptorSetLayout, pipelineLayout, pipelinePointer[0])
                        } finally {
                            VK10.vkDestroyShaderModule(device.vkDevice(), shaderModule, null)
                        }
                    } catch (error: RuntimeException) {
                        VK10.vkDestroyPipelineLayout(device.vkDevice(), pipelineLayout, null)
                        throw error
                    }
                } catch (error: RuntimeException) {
                    VK10.vkDestroyDescriptorSetLayout(device.vkDevice(), descriptorSetLayout, null)
                    throw error
                }
            }
        }

        private fun checkVk(result: Int) = check(result == VK10.VK_SUCCESS) { "Vulkan error $result" }

        const val STATS_SIZE = 16
        private const val WORKGROUP_SIZE = 64
        private const val DESCRIPTOR_COUNT = 5
        private const val PROJECTION_BINDING = 4
        private const val PUSH_CONSTANT_SIZE = 96
        private const val SHADER_PATH = "/assets/libgltf/shaders/compute/gpu_cull.comp"
    }
}
