package com.micheanl.libgltf.mixin.iris;

import com.micheanl.libgltf.render.iris.IrisPbrTextures;
import net.irisshaders.iris.pbr.texture.PBRTextureHolder;
import net.irisshaders.iris.pbr.texture.PBRTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PBRTextureManager.class, remap = false)
/**
 * libgltf · PbrTextureManagerMixin
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

public abstract class PbrTextureManagerMixin {
    @Inject(method = {"getHolder", "getOrLoadHolder"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void libgltf$getHolder(int id, CallbackInfoReturnable<PBRTextureHolder> callback) {
        PBRTextureHolder holder = IrisPbrTextures.get(id);
        if (holder != null) {
            callback.setReturnValue(holder);
        }
    }
}
