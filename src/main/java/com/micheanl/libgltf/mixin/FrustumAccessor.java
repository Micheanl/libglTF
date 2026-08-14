package com.micheanl.libgltf.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Frustum.class)

/**
 * libgltf · FrustumAccessor
 *
 * ```
 * camera.cullFrustum.libgltf$intersection().testAab(minX, minY, minZ, maxX, maxY, maxZ)
 * ```
 *
 * 暴露视锥体 JOML 相交测试，免去每节点 AABB 对象分配
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

public interface FrustumAccessor {
    @Accessor("intersection")
    FrustumIntersection libgltf$intersection();
}
