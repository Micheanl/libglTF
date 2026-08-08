package com.micheanl.libgltf.animation

data class AnimationChannel(
    val nodeIndex: Int,
    val path: AnimationPath,
    val interpolation: Interpolation,
    val times: FloatArray,
    val values: FloatArray,
    val componentCount: Int,
    val materialIndex: Int = -1,
    val textureSlot: Int = 0,
    val textureProperty: Int = 0
)
