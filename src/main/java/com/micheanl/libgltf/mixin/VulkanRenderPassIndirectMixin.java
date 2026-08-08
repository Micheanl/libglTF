package com.micheanl.libgltf.mixin;

import com.micheanl.libgltf.render.gpu.MeshletStorage;
import com.micheanl.libgltf.render.vulkan.VulkanMeshCache;
import com.micheanl.libgltf.render.vulkan.VulkanIndirectRenderPass;
import com.micheanl.libgltf.render.vulkan.VulkanMeshRenderPass;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import org.jspecify.annotations.Nullable;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassIndirectMixin implements VulkanIndirectRenderPass, VulkanMeshRenderPass {
    @Shadow
    protected @Nullable VulkanRenderPipeline pipeline;

    @Shadow
    private boolean hasDepth;

    @Shadow
    private VkCommandBuffer commandBuffer() {
        throw new AssertionError();
    }

    @Shadow
    private void pushDescriptors() {
        throw new AssertionError();
    }

    @Override
    public void drawIndexedIndirectCount(GpuBufferSlice commands, GpuBufferSlice count, int maxDrawCount) {
        if (pipeline == null || pipeline.isClosed()) {
            throw new IllegalStateException("Pipeline is missing or not valid");
        }
        pushDescriptors();
        VK12.vkCmdDrawIndexedIndirectCount(
                commandBuffer(),
                ((VulkanGpuBuffer) commands.buffer()).vkBuffer(),
                commands.offset(),
                ((VulkanGpuBuffer) count.buffer()).vkBuffer(),
                count.offset(),
                maxDrawCount,
                VkDrawIndexedIndirectCommand.SIZEOF
        );
    }

    @Override
    public boolean drawMeshTasks(
            VulkanMeshCache cache,
            RenderPipeline renderPipeline,
            GpuBuffer geometry,
            GpuBuffer instances,
            MeshletStorage meshlets,
            float[] sphere,
            int instanceCount,
            boolean instanceCulling,
            boolean meshletCulling
    ) {
        if (pipeline == null || pipeline.isClosed()) {
            return false;
        }
        VulkanRenderPipeline original = pipeline;
        VulkanRenderPipeline descriptorPipeline = cache.descriptorPipeline(renderPipeline, original);
        if (descriptorPipeline == null) {
            return false;
        }
        pipeline = descriptorPipeline;
        try {
            pushDescriptors();
        } finally {
            pipeline = original;
        }
        boolean drawn = cache.draw(
                renderPipeline,
                original,
                commandBuffer(),
                hasDepth,
                geometry,
                instances,
                meshlets,
                sphere,
                instanceCount,
                instanceCulling,
                meshletCulling
        );
        if (drawn) {
            VK10.vkCmdBindPipeline(
                    commandBuffer(),
                    VK10.VK_PIPELINE_BIND_POINT_GRAPHICS,
                    hasDepth || original.withoutDepthPipeline() == 0L
                            ? original.withDepthPipeline()
                            : original.withoutDepthPipeline()
            );
        }
        return drawn;
    }
}
