package com.micheanl.libgltf.material

data class PbrSpecularGlossinessMaterial(
    val diffuseFactor: FloatArray,
    val diffuseTexture: TextureBinding?,
    val specularFactor: FloatArray,
    val specularGlossinessTexture: TextureBinding?,
    val glossinessFactor: Float
)
