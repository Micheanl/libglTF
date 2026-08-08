package com.micheanl.libgltf.render

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.util.LightCoordsUtil
import net.minecraft.util.Mth
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.joml.Matrix4f

object GltfWorldRenderer {
    private val relativeTransform = Matrix4f()
    private val lightPosition = BlockPos.MutableBlockPos()

    fun submit(context: LevelRenderContext) {
        val instances = GltfFrameState.instances()
        if (instances.isEmpty()) return
        val level = Minecraft.getInstance().level ?: return
        val camera = context.levelState().cameraRenderState.pos
        val poseStack = context.poseStack()
        val collector = context.submitNodeCollector()
        for (instance in instances) {
            if (!instance.visible || instance.handle.isClosed) continue
            val x = instance.transform.m30()
            val y = instance.transform.m31()
            val z = instance.transform.m32()
            val dx = x - camera.x.toFloat()
            val dy = y - camera.y.toFloat()
            val dz = z - camera.z.toFloat()
            val distanceSquared = dx * dx + dy * dy + dz * dz
            val light = light(level, x, y, z)
            relativeTransform.set(instance.transform)
            relativeTransform.m30(dx).m31(dy).m32(dz)
            GltfSceneRenderer.submit(instance, poseStack, collector, light, distanceSquared = distanceSquared, transform = relativeTransform)
        }
    }

    private fun light(level: ClientLevel, x: Float, y: Float, z: Float): Int {
        lightPosition.set(Mth.floor(x), Mth.floor(y), Mth.floor(z))
        val chunk = level.getChunk(lightPosition.x shr 4, lightPosition.z shr 4, ChunkStatus.FULL, false)
        return if (chunk != null) LightCoordsUtil.getLightCoords(level, lightPosition) else LightCoordsUtil.FULL_BRIGHT
    }
}
