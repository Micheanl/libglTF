package com.micheanl.libgltf.render.debug

import com.micheanl.libgltf.api.GltfInstance
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.SubmitNodeCollector
import kotlin.math.sqrt

class GltfBoneDebugRenderer(private val instance: GltfInstance) : SubmitNodeCollector.CustomGeometryRenderer {
    override fun render(pose: PoseStack.Pose, buffer: VertexConsumer) {
        val asset = instance.handle.asset
        val jointMatrices = instance.animation.pose.globalMatrices
        for (skin in asset.skins) {
            val inSkin = BooleanArray(asset.nodes.size)
            for (joint in skin.joints) inSkin[joint] = true
            for (joint in skin.joints) {
                val node = asset.nodes[joint]
                val matrix = jointMatrices[joint]
                val startX = matrix.m30()
                val startY = matrix.m31()
                val startZ = matrix.m32()
                for (child in node.children) {
                    if (!inSkin[child]) continue
                    val childMatrix = jointMatrices[child]
                    val endX = childMatrix.m30()
                    val endY = childMatrix.m31()
                    val endZ = childMatrix.m32()
                    line(pose, buffer, startX, startY, startZ, endX, endY, endZ)
                }
            }
        }
    }

    private fun line(
        pose: PoseStack.Pose,
        buffer: VertexConsumer,
        startX: Float,
        startY: Float,
        startZ: Float,
        endX: Float,
        endY: Float,
        endZ: Float
    ) {
        val directionX = endX - startX
        val directionY = endY - startY
        val directionZ = endZ - startZ
        val length = sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ)
        val normalX = if (length > 0.0f) directionX / length else 0.0f
        val normalY = if (length > 0.0f) directionY / length else 0.0f
        val normalZ = if (length > 0.0f) directionZ / length else 0.0f
        buffer.addVertex(pose, startX, startY, startZ)
            .setColor(BONE_COLOR)
            .setNormal(pose, normalX, normalY, normalZ)
            .setLineWidth(1.0f)
        buffer.addVertex(pose, endX, endY, endZ)
            .setColor(BONE_COLOR)
            .setNormal(pose, normalX, normalY, normalZ)
            .setLineWidth(1.0f)
    }

    private companion object {
        const val BONE_COLOR: Int = 0xFF00FF00.toInt()
    }
}
