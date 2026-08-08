package com.micheanl.libgltf.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VulkanCommandEncoder.class)

/**
 * libgltf · VulkanCommandEncoderAccessor
 *
 * ```
 * val commandBuffer = (device.createCommandEncoder() as VulkanCommandEncoderAccessor).`libgltf$commandBuffer`()
 * ```
 *
 * 获取 Vulkan 命令缓冲的 mixin 访问器
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

public interface VulkanCommandEncoderAccessor {
    @Invoker("commandBuffer")
    VkCommandBuffer libgltf$commandBuffer();
}
