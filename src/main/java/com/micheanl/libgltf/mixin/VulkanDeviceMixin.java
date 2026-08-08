package com.micheanl.libgltf.mixin;

/**
 * libgltf · VulkanDeviceMixin
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import com.micheanl.libgltf.render.gpu.VertexAttributeLimitProvider;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanInstance;
import com.mojang.renderpearl.backend.vulkan.VulkanPhysicalDevice;
import com.mojang.renderpearl.backend.vulkan.checkpoints.CheckpointExtension;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import org.lwjgl.vulkan.VkDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanDevice.class)
public abstract class VulkanDeviceMixin implements VertexAttributeLimitProvider {
    @Unique
    private int libgltf$maxVertexAttributes = VertexFormat.MAX_VERTEX_ELEMENTS;

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanPhysicalDevice;close()V",
                    shift = At.Shift.BEFORE
            ),
            require = 1
    )
    private void libgltf$captureVertexAttributeLimit(
            VulkanInstance instance,
            VulkanPhysicalDevice physicalDevice,
            FeatureSet enabledFeatureSet,
            VkDevice vkDevice,
            long vma,
            CheckpointExtension checkpointExtension,
            CallbackInfo callback
    ) {
        libgltf$maxVertexAttributes = physicalDevice.vkPhysicalDeviceProperties().limits().maxVertexInputAttributes();
    }

    @Override
    public int getMaxVertexAttributes() {
        return libgltf$maxVertexAttributes;
    }
}
