package com.micheanl.libgltf.render.texture

import com.mojang.blaze3d.platform.NativeImage
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil




/**
 * libgltf · NativeImageDecoder
 *
 * ```
 * val image = asset.images.getOrNull(texture.imageIndex)?.let { NativeImageDecoder.decode(it.bytes) } ?: whiteImage()
 * ```
 *
 * 原生图像解码
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object NativeImageDecoder {
    fun decode(bytes: ByteArray): NativeImage? {
        if (bytes.isEmpty()) return null
        val file = MemoryUtil.memAlloc(bytes.size)
        return try {
            file.put(bytes).flip()
            MemoryStack.stackPush().use { stack ->
                val width = stack.mallocInt(1)
                val height = stack.mallocInt(1)
                val channels = stack.mallocInt(1)
                val pixels = STBImage.stbi_load_from_memory(file, width, height, channels, 4) ?: return null
                NativeImage(
                    NativeImage.Format.RGBA,
                    width[0],
                    height[0],
                    true,
                    MemoryUtil.memAddress(pixels)
                )
            }
        } finally {
            MemoryUtil.memFree(file)
        }
    }
}
