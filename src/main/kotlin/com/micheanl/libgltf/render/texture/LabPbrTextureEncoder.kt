package com.micheanl.libgltf.render.texture

import com.micheanl.libgltf.material.GltfMaterial
import com.micheanl.libgltf.material.TextureBinding
import com.micheanl.libgltf.model.GltfAsset
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.util.Mth
import org.lwjgl.system.MemoryUtil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt


/**
 * libgltf · LabPbrTextureEncoder
 *
 * <pre><code>
 * LabPbrTextureEncoder.normal(asset, material),
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object LabPbrTextureEncoder {
    private const val DIELECTRIC_G_MAX: Int = 229

    fun normal(asset: GltfAsset, material: GltfMaterial): NativeImage {
        val normal = image(asset, material.normalTexture)
        val occlusion = image(asset, material.occlusionTexture)
        try {
            val width = normal?.width ?: occlusion?.width ?: 1
            val height = normal?.height ?: occlusion?.height ?: 1
            val output = NativeImage(width, height, false)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val normalPixel = normal?.let { pixel(it, x, y, width, height) } ?: 0xFFFF8080.toInt()
                    val normalX = Mth.clamp((((normalPixel and 0xFF) / 255.0f) * 2.0f - 1.0f) * material.normalScale, -1.0f, 1.0f)
                    val normalY = Mth.clamp(((((normalPixel ushr 8) and 0xFF) / 255.0f) * 2.0f - 1.0f) * material.normalScale, -1.0f, 1.0f)
                    val red = unorm(normalX * 0.5f + 0.5f)
                    val green = unorm(normalY * 0.5f + 0.5f)
                    val sourceOcclusion = occlusion?.let { (pixel(it, x, y, width, height) and 0xFF) / 255.0f } ?: 1.0f
                    val blue = unorm(1.0f + material.occlusionStrength * (sourceOcclusion - 1.0f))
                    output.setPixelABGR(x, y, -0x1000000 or (blue shl 16) or (green shl 8) or red)
                }
            }
            return output
        } finally {
            normal?.close()
            occlusion?.close()
        }
    }

    fun specular(asset: GltfAsset, material: GltfMaterial): NativeImage {
        val metallicRoughness = image(asset, material.metallicRoughnessTexture)
        val specularTexture = image(asset, material.specular?.texture)
        val emissive = image(asset, material.emissiveTexture)
        try {
            val width = metallicRoughness?.width ?: specularTexture?.width ?: emissive?.width ?: 1
            val height = metallicRoughness?.height ?: specularTexture?.height ?: emissive?.height ?: 1
            val output = NativeImage(width, height, false)
            val specular = material.specular
            val color = specular?.colorFactor
            val colorMaximum = if (color == null || color.isEmpty()) 1.0f else max(color[0], max(color.getOrElse(1) { 1.0f }, color.getOrElse(2) { 1.0f }))
            val reflectance = (1.5f - 1.0f) / (1.5f + 1.0f)
            val baseF0 = reflectance * reflectance * (specular?.factor ?: 1.0f) * colorMaximum
            val clearcoat = material.clearcoat
            val clearcoatFactor = clearcoat?.factor ?: 0.0f
            val clearcoatSmoothness = 1.0f - sqrt(Mth.clamp(clearcoat?.roughnessFactor ?: 0.0f, 0.0f, 1.0f))
            val sheen = material.sheen
            val sheenColor = sheen?.colorFactor
            val sheenBoost = if (sheenColor == null || sheenColor.size < 3) {
                0.0f
            } else {
                luminance(sheenColor[0], sheenColor[1], sheenColor[2]) *
                    (1.0f - Mth.clamp(sheen.roughnessFactor, 0.0f, 1.0f)) * 0.25f
            }
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val metallicRoughnessPixel = metallicRoughness?.let { pixel(it, x, y, width, height) }
                    val roughness = if (metallicRoughnessPixel == null) {
                        material.roughnessFactor
                    } else {
                        ((metallicRoughnessPixel ushr 8) and 0xFF) / 255.0f * material.roughnessFactor
                    }
                    val metallic = if (metallicRoughnessPixel == null) {
                        material.metallicFactor
                    } else {
                        ((metallicRoughnessPixel ushr 16) and 0xFF) / 255.0f * material.metallicFactor
                    }
                    var smoothness = 1.0f - sqrt(Mth.clamp(roughness, 0.0f, 1.0f))
                    if (clearcoatFactor > 0.0f) {
                        smoothness += (max(smoothness, clearcoatSmoothness) - smoothness) * clearcoatFactor
                    }
                    smoothness += sheenBoost
                    val red = unorm(smoothness)
                    val green = if (metallic >= 0.5f) {
                        255
                    } else {
                        var f0 = baseF0
                        if (specularTexture != null) {
                            f0 *= ((pixel(specularTexture, x, y, width, height) ushr 24) and 0xFF) / 255.0f
                        }
                        if (clearcoatFactor > 0.0f) f0 += (max(f0, 0.04f) - f0) * clearcoatFactor
                        unorm(f0).coerceAtMost(DIELECTRIC_G_MAX)
                    }
                    val alpha = if (emissive == null) {
                        0
                    } else {
                        val emissivePixel = pixel(emissive, x, y, width, height)
                        val factor = material.emissiveFactor
                        val redFactor = factor.getOrElse(0) { 0.0f }
                        val greenFactor = factor.getOrElse(1) { 0.0f }
                        val blueFactor = factor.getOrElse(2) { 0.0f }
                        val luminance = luminance(
                            (emissivePixel and 0xFF) / 255.0f * redFactor,
                            ((emissivePixel ushr 8) and 0xFF) / 255.0f * greenFactor,
                            ((emissivePixel ushr 16) and 0xFF) / 255.0f * blueFactor
                        ) * material.emissiveStrength
                        (Mth.clamp(luminance, 0.0f, 1.0f) * 254.0f).roundToInt()
                    }
                    output.setPixelABGR(x, y, (alpha shl 24) or (green shl 8) or red)
                }
            }
            return output
        } finally {
            metallicRoughness?.close()
            specularTexture?.close()
            emissive?.close()
        }
    }

    private fun image(asset: GltfAsset, binding: TextureBinding?): NativeImage? {
        val textureIndex = binding?.textureIndex ?: return null
        val texture = asset.textures.getOrNull(textureIndex) ?: return null
        val bytes = asset.images.getOrNull(texture.imageIndex)?.bytes ?: return null
        return NativeImageDecoder.decode(bytes)
    }

    private fun pixel(image: NativeImage, x: Int, y: Int, width: Int, height: Int): Int {
        val sourceX = x * image.width / width
        val sourceY = y * image.height / height
        return MemoryUtil.memGetInt(image.pointer + (sourceY * image.width + sourceX) * 4L)
    }

    private fun luminance(red: Float, green: Float, blue: Float): Float =
        0.2126f * red + 0.7152f * green + 0.0722f * blue

    private fun unorm(value: Float): Int = (Mth.clamp(value, 0.0f, 1.0f) * 255.0f).roundToInt()
}
