package com.micheanl.libgltf.material

/**
 * libgltf · MaterialUvAnimationState
 *
 * ```
 * private val restMaterialUv = MaterialUvAnimationState(asset.materials.size)
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class MaterialUvAnimationState(private val materialCount: Int) {
    val animated = BooleanArray(materialCount)
    val offsetX = FloatArray(materialCount)
    val offsetY = FloatArray(materialCount)
    val rotation = FloatArray(materialCount)
    val scaleX = FloatArray(materialCount) { 1.0f }
    val scaleY = FloatArray(materialCount) { 1.0f }

    fun reset() {
        animated.fill(false)
        offsetX.fill(0.0f)
        offsetY.fill(0.0f)
        rotation.fill(0.0f)
        scaleX.fill(1.0f)
        scaleY.fill(1.0f)
    }

    fun copyFrom(source: MaterialUvAnimationState) {
        source.animated.copyInto(animated)
        source.offsetX.copyInto(offsetX)
        source.offsetY.copyInto(offsetY)
        source.rotation.copyInto(rotation)
        source.scaleX.copyInto(scaleX)
        source.scaleY.copyInto(scaleY)
    }

    fun blendFrom(first: MaterialUvAnimationState, second: MaterialUvAnimationState, factor: Float) {
        for (index in 0 until materialCount) {
            animated[index] = first.animated[index] || second.animated[index]
            offsetX[index] = first.offsetX[index] + (second.offsetX[index] - first.offsetX[index]) * factor
            offsetY[index] = first.offsetY[index] + (second.offsetY[index] - first.offsetY[index]) * factor
            rotation[index] = first.rotation[index] + (second.rotation[index] - first.rotation[index]) * factor
            scaleX[index] = first.scaleX[index] + (second.scaleX[index] - first.scaleX[index]) * factor
            scaleY[index] = first.scaleY[index] + (second.scaleY[index] - first.scaleY[index]) * factor
        }
    }
}
