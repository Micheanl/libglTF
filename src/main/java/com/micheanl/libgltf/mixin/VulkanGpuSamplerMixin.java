package com.micheanl.libgltf.mixin;

/**
 * libgltf · VulkanGpuSamplerMixin
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.render.texture.MipmapFilterState;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuSampler;
import org.lwjgl.vulkan.VK10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(VulkanGpuSampler.class)
public abstract class VulkanGpuSamplerMixin {
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VkSamplerCreateInfo;mipmapMode(I)Lorg/lwjgl/vulkan/VkSamplerCreateInfo;",
                    remap = false
            )
    )
    private int libgltf$mipmapMode(int original) {
        return MipmapFilterState.nearest() ? VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST : original;
    }
}
