package com.micheanl.libgltf.material

/**
 * libgltf · GltfMaterial
 *
 * glTF 材质与扩展
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GltfMaterial(
    val name: String,
    val baseColorFactor: FloatArray,
    val baseColorTexture: TextureBinding?,
    val metallicFactor: Float,
    val roughnessFactor: Float,
    val metallicRoughnessTexture: TextureBinding?,
    val normalTexture: TextureBinding?,
    val normalScale: Float,
    val occlusionTexture: TextureBinding?,
    val occlusionStrength: Float,
    val emissiveTexture: TextureBinding?,
    val emissiveFactor: FloatArray,
    val emissiveStrength: Float,
    val alphaMode: AlphaMode,
    val alphaCutoff: Float,
    val doubleSided: Boolean,
    val unlit: Boolean,
    val specular: SpecularMaterial?,
    val clearcoat: ClearcoatMaterial?,
    val sheen: SheenMaterial?,
    val transmissionFactor: Float,
    val transmissionTexture: TextureBinding?,
    val thicknessFactor: Float,
    val thicknessTexture: TextureBinding?,
    val attenuationDistance: Float,
    val attenuationColor: FloatArray,
    val ior: Float,
    val dispersion: Float,
    val anisotropy: AnisotropyMaterial?,
    val iridescence: IridescenceMaterial?
)
