package com.micheanl.libgltf.lod

/**
 * libgltf · LodSelector
 *
 * ```
 * lodSelector = LodSelector(value)
 * ```
 *
 * LOD 距离选择
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class LodSelector(private val policy: LodPolicy) {
    fun select(distanceSquared: Float, previousLevel: Int): Int {
        val distances = policy.levelDistances
        val current = previousLevel.coerceIn(0, distances.lastIndex)
        if (current > 0) {
            val boundary = distances[current] * (1.0f - policy.hysteresis)
            if (distanceSquared < boundary * boundary) {
                return current - 1
            }
        }
        if (current < distances.lastIndex) {
            val boundary = distances[current + 1] * (1.0f + policy.hysteresis)
            if (distanceSquared >= boundary * boundary) {
                return current + 1
            }
        }
        return current
    }

    fun visible(distanceSquared: Float): Boolean = distanceSquared <= policy.renderDistance * policy.renderDistance

    fun animate(distanceSquared: Float): Boolean = distanceSquared <= policy.animationDistance * policy.animationDistance

    fun transparent(distanceSquared: Float): Boolean = distanceSquared <= policy.transparencyDistance * policy.transparencyDistance
}
