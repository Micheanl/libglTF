package com.micheanl.libgltf.mixin;

import com.micheanl.libgltf.render.gpu.GltfOcclusionDepth;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererOcclusionMixin {
    @Inject(
            method = "executeOutline(Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;)V",
            at = @At("RETURN")
    )
    private void libgltf$copyOcclusionDepth(
            FeatureRenderDispatcher.PreparedFrame featureFrame,
            CallbackInfo ci
    ) {
        GltfOcclusionDepth.INSTANCE.update();
    }
}
