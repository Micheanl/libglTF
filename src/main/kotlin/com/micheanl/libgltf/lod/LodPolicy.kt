package com.micheanl.libgltf.lod




/**
 * libgltf · LodPolicy
 *
 * ```
 * fun load(path: Path, lodPolicy: LodPolicy = LodPolicy.DEFAULT): GltfLoadResult
 * ```
 *
 * LOD 策略配置
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class LodPolicy(
    val renderDistance: Float,
    val animationDistance: Float,
    val transparencyDistance: Float,
    val levelDistances: FloatArray,
    val triangleRatios: FloatArray,
    val hysteresis: Float
) {
    init {
        require(renderDistance > 0.0f)
        require(animationDistance > 0.0f)
        require(transparencyDistance > 0.0f)
        require(levelDistances.isNotEmpty())
        require(levelDistances.size == triangleRatios.size)
        require(hysteresis in 0.0f..0.5f)
    }

    companion object {
        @JvmField
        val DEFAULT: LodPolicy = LodPolicy(
            256.0f,
            96.0f,
            128.0f,
            floatArrayOf(0.0f, 48.0f, 128.0f),
            floatArrayOf(1.0f, 0.35f, 0.1f),
            0.1f
        )
    }
}
