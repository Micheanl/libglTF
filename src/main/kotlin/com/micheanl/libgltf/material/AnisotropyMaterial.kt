package com.micheanl.libgltf.material

data class AnisotropyMaterial(
    val strength: Float,
    val rotation: Float,
    val texture: TextureBinding?
)
