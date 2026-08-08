package com.micheanl.libgltf.material




/**
 * libgltf · MaterialFactorAnimationState
 *
 * ```
 * private val restMaterialFactor = MaterialFactorAnimationState(asset.materials.size)
 * ```
 *
 * 材质系数动画状态
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class MaterialFactorAnimationState(private val materialCount: Int) {
    val animated = BooleanArray(materialCount)
    val baseColorFactor = FloatArray(materialCount * 4)

    fun reset() {
        animated.fill(false)
        baseColorFactor.fill(1.0f)
    }

    fun copyFrom(source: MaterialFactorAnimationState) {
        source.animated.copyInto(animated)
        source.baseColorFactor.copyInto(baseColorFactor)
    }

    fun blendFrom(first: MaterialFactorAnimationState, second: MaterialFactorAnimationState, factor: Float) {
        for (index in 0 until materialCount) {
            animated[index] = first.animated[index] || second.animated[index]
            val base = index * 4
            baseColorFactor[base] =
                first.baseColorFactor[base] + (second.baseColorFactor[base] - first.baseColorFactor[base]) * factor
            baseColorFactor[base + 1] =
                first.baseColorFactor[base + 1] + (second.baseColorFactor[base + 1] - first.baseColorFactor[base + 1]) * factor
            baseColorFactor[base + 2] =
                first.baseColorFactor[base + 2] + (second.baseColorFactor[base + 2] - first.baseColorFactor[base + 2]) * factor
            baseColorFactor[base + 3] =
                first.baseColorFactor[base + 3] + (second.baseColorFactor[base + 3] - first.baseColorFactor[base + 3]) * factor
        }
    }
}
