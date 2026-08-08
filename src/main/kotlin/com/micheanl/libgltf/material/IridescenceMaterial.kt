package com.micheanl.libgltf.material

data class IridescenceMaterial(
    val factor: Float,
    val texture: TextureBinding?,
    val ior: Float,
    val thicknessMinimum: Float,
    val thicknessMaximum: Float,
    val thicknessTexture: TextureBinding?
)
