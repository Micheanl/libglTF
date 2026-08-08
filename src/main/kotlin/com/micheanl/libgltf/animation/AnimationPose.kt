package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationPose
 *
 * <pre>{@code
 * val pose: AnimationPose = AnimationPose(asset.nodes.size, asset.totalMorphWeights, asset.materials.size)
 * }</pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.material.MaterialUvAnimationState
import com.micheanl.libgltf.material.MaterialFactorAnimationState
import org.joml.Matrix4f

class AnimationPose(nodeCount: Int, morphWeightCount: Int, materialCount: Int) {
    val localMatrices: Array<Matrix4f> = Array(nodeCount) { Matrix4f() }
    val globalMatrices: Array<Matrix4f> = Array(nodeCount) { Matrix4f() }
    val morphWeights: FloatArray = FloatArray(morphWeightCount)
    val materialUv: MaterialUvAnimationState = MaterialUvAnimationState(materialCount)
    val materialFactor: MaterialFactorAnimationState = MaterialFactorAnimationState(materialCount)
}
