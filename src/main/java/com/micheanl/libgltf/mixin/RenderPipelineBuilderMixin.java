package com.micheanl.libgltf.mixin;

import com.micheanl.libgltf.LibGltf;
import com.micheanl.libgltf.render.gpu.GltfGpuBackend;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(RenderPipeline.Builder.class)
public abstract class RenderPipelineBuilderMixin {
    @Shadow
    private Optional<Identifier> location;

    @ModifyConstant(
            method = "build",
            constant = @Constant(intValue = 16),
            require = 1
    )
    private int libgltf$vertexAttributeLimit(int original) {
        Identifier pipelineLocation = location.orElse(null);
        return pipelineLocation != null && LibGltf.MOD_ID.equals(pipelineLocation.getNamespace())
                ? GltfGpuBackend.INSTANCE.vertexAttributeLimit()
                : original;
    }
}
