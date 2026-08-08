package com.micheanl.libgltf.mixin;

import com.micheanl.libgltf.render.vulkan.GltfVulkanUsage;
import com.mojang.renderpearl.backend.vulkan.VulkanConst;
import org.lwjgl.vulkan.VK10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanConst.class)
public abstract class VulkanConstMixin {
    @Inject(method = "bufferUsageToVk", at = @At("RETURN"), cancellable = true, require = 1)
    private static void libgltf$storageBufferUsage(int usage, CallbackInfoReturnable<Integer> callback) {
        if ((usage & GltfVulkanUsage.STORAGE) != 0) {
            callback.setReturnValue(callback.getReturnValue() | VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
        }
    }
}