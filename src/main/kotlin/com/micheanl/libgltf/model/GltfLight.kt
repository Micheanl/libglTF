package com.micheanl.libgltf.model

data class GltfLight(
    val name: String,
    val type: LightType,
    val color: FloatArray,
    val intensity: Float,
    val range: Float,
    val spotInnerConeAngle: Float,
    val spotOuterConeAngle: Float
)
