package com.micheanl.libgltf.render.gpu

import com.micheanl.libgltf.animation.AnimationPose
import com.micheanl.libgltf.model.GltfAsset
import org.joml.Matrix4f

/**
 * libgltf · GpuAnimationState
 *
 * ```
 * val animationState: GpuAnimationState = GpuAnimationState(handle.asset)
 * ```
 *
 * 动画姿态的 GPU 表示
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GpuAnimationState(private val asset: GltfAsset) {
    val jointPalettes: Array<FloatArray> = Array(asset.skins.size) { index -> FloatArray(asset.skins[index].joints.size * 16) }
    val morphWeights: FloatArray = FloatArray(asset.totalMorphWeights)
    private val inverseBind = Matrix4f()
    private val joint = Matrix4f()

    fun update(pose: AnimationPose) {
        pose.morphWeights.copyInto(morphWeights)
        for (skinIndex in asset.skins.indices) {
            val skin = asset.skins[skinIndex]
            val palette = jointPalettes[skinIndex]
            for (jointIndex in skin.joints.indices) {
                inverseBind.set(skin.inverseBindMatrices, jointIndex * 16)
                joint.set(pose.globalMatrices[skin.joints[jointIndex]]).mul(inverseBind)
                joint.get(palette, jointIndex * 16)
            }
        }
    }
}
