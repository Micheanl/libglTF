package com.micheanl.libgltf.animation

import com.micheanl.libgltf.material.MaterialUvAnimationState
import org.joml.Matrix4f

class AnimationPose(nodeCount: Int, morphWeightCount: Int, materialCount: Int) {
    val localMatrices: Array<Matrix4f> = Array(nodeCount) { Matrix4f() }
    val globalMatrices: Array<Matrix4f> = Array(nodeCount) { Matrix4f() }
    val morphWeights: FloatArray = FloatArray(morphWeightCount)
    val materialUv: MaterialUvAnimationState = MaterialUvAnimationState(materialCount)
}
