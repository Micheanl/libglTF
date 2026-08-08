package com.micheanl.libgltf.mixin;

import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FrontendGpuDevice.class)
/**
 * libgltf · FrontendGpuDeviceAccessor
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

public interface FrontendGpuDeviceAccessor {
    @Accessor("backend")
    GpuDeviceBackend getLibgltfBackend();
}
