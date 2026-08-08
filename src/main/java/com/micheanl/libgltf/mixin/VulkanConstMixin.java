package com.micheanl.libgltf.mixin;

import com.micheanl.libgltf.render.vulkan.VulkanUsage;
import com.mojang.renderpearl.backend.vulkan.VulkanConst;
import org.lwjgl.vulkan.VK10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanConst.class)

/**
 * libgltf · VulkanConstMixin
 *
 * ```
 * @Mixin(VulkanConst.class)
 * ```
 *
 * Vulkan 格式常量转换 mixin
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

public abstract class VulkanConstMixin {
    @Inject(method = "bufferUsageToVk", at = @At("RETURN"), cancellable = true, require = 1)
    private static void libgltf$storageBufferUsage(int usage, CallbackInfoReturnable<Integer> callback) {
        if ((usage & VulkanUsage.STORAGE) != 0) {
            callback.setReturnValue(callback.getReturnValue() | VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
        }
    }
}
