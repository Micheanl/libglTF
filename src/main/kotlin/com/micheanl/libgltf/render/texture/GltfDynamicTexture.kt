package com.micheanl.libgltf.render.texture

/**
 * libgltf · GltfDynamicTexture
 *
 * <pre>{@code
 * val dynamic = GltfDynamicTexture(identifiers[index].toString(), image, texture.sampler)
 * }</pre>
 *
 * 动态更新的 GPU 纹理
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.material.TextureFilter
import com.micheanl.libgltf.material.TextureSampler
import com.micheanl.libgltf.material.TextureWrap
import com.mojang.renderpearl.api.GpuFormat
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.textures.AddressMode
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.logging.LogUtils
import net.minecraft.client.renderer.texture.AbstractTexture
import java.util.OptionalDouble
import kotlin.math.max

class GltfDynamicTexture(
    label: String,
    image: NativeImage,
    samplerDefinition: TextureSampler
) : AbstractTexture() {
    private val levels: Array<NativeImage> = createLevels(image, samplerDefinition.mipmap != null)
    private val ownsSampler: Boolean = samplerDefinition.mipmap == TextureFilter.NEAREST

    init {
        val device = RenderSystem.getDevice()
        val gpuTexture = device.createTexture(
            { label },
            GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
            GpuFormat.RGBA8_UNORM,
            image.width,
            image.height,
            1,
            levels.size
        )
        texture = gpuTexture
        textureView = device.createTextureView(gpuTexture)
        sampler = if (ownsSampler) {
            MipmapFilterState.create(TextureFilter.NEAREST) {
                device.createSampler(
                    address(samplerDefinition.wrapS),
                    address(samplerDefinition.wrapT),
                    filter(samplerDefinition.minification),
                    filter(samplerDefinition.magnification),
                    1,
                    OptionalDouble.empty()
                )
            }
        } else {
            RenderSystem.getSamplerCache().getSampler(
                address(samplerDefinition.wrapS),
                address(samplerDefinition.wrapT),
                filter(samplerDefinition.minification),
                filter(samplerDefinition.magnification),
                samplerDefinition.mipmap != null
            )
        }
        val encoder = device.createCommandEncoder()
        for (level in levels.indices) {
            val expectedWidth = gpuTexture.getWidth(level)
            val expectedHeight = gpuTexture.getHeight(level)
            val source = levels[level]
            if (expectedWidth <= 0 || expectedHeight <= 0) {
                LOGGER.warn(
                    "libgltf texture {} mip {} has invalid GPU size {}x{}, skipping",
                    label,
                    level,
                    expectedWidth,
                    expectedHeight
                )
                continue
            }
            if (source.width == expectedWidth && source.height == expectedHeight) {
                encoder.writeToTexture(gpuTexture, source, level, 0, 0, 0)
            } else {
                LOGGER.warn(
                    "libgltf texture {} mip {} size {}x{} does not match GPU mip {}x{}, resizing",
                    label,
                    level,
                    source.width,
                    source.height,
                    expectedWidth,
                    expectedHeight
                )
                NativeImage(expectedWidth, expectedHeight, false).use { resized ->
                    source.resizeSubRectTo(0, 0, source.width, source.height, resized)
                    encoder.writeToTexture(gpuTexture, resized, level, 0, 0, 0)
                }
            }
        }
    }

    override fun close() {
        for (level in levels) level.close()
        super.close()
        if (ownsSampler) sampler.close()
    }

    private companion object {
        val LOGGER = LogUtils.getLogger()

        fun createLevels(image: NativeImage, mipmaps: Boolean): Array<NativeImage> {
            if (!mipmaps) return arrayOf(image)
            var width = image.width
            var height = image.height
            var count = 1
            while (width > 1 && height > 1) {
                width = max(1, width / 2)
                height = max(1, height / 2)
                count++
            }
            val output = arrayOfNulls<NativeImage>(count)
            output[0] = image
            for (level in 1 until count) {
                val previous = requireNotNull(output[level - 1])
                output[level] = NativeImage(max(1, previous.width / 2), max(1, previous.height / 2), false).also {
                    previous.resizeSubRectTo(0, 0, previous.width, previous.height, it)
                }
            }
            return Array(count) { requireNotNull(output[it]) }
        }

        fun address(value: TextureWrap): AddressMode = when (value) {
            TextureWrap.CLAMP_TO_EDGE -> AddressMode.CLAMP_TO_EDGE
            TextureWrap.MIRRORED_REPEAT, TextureWrap.REPEAT -> AddressMode.REPEAT
        }

        fun filter(value: TextureFilter): FilterMode = when (value) {
            TextureFilter.NEAREST -> FilterMode.NEAREST
            TextureFilter.LINEAR -> FilterMode.LINEAR
        }
    }
}
