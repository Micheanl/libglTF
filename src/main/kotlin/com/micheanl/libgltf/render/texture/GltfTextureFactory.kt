package com.micheanl.libgltf.render.texture

import com.micheanl.libgltf.LibGltf
import com.micheanl.libgltf.material.GltfMaterial
import com.micheanl.libgltf.material.TextureBinding
import com.micheanl.libgltf.material.TextureSampler
import com.micheanl.libgltf.model.GltfAsset
import com.micheanl.libgltf.render.iris.IrisCompat
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier


/**
 * libgltf · GltfTextureFactory
 *
 * <pre><code>
 * textures = GltfTextureFactory.create(asset, id)
 * </code></pre>
 *
 * 纹理创建
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object GltfTextureFactory {
    fun create(asset: GltfAsset, resourceId: Long): GltfTextureSet {
        val manager = Minecraft.getInstance().textureManager
        val identifiers = Array(asset.textures.size) { index ->
            LibGltf.id("runtime/$resourceId/texture_$index")
        }
        val textures = arrayOfNulls<GltfDynamicTexture>(identifiers.size)
        val registered = BooleanArray(identifiers.size)
        val fallback = LibGltf.id("runtime/$resourceId/fallback")
        var fallbackTexture: GltfDynamicTexture? = null
        var fallbackRegistered = false
        val materialIdentifiers = Array(asset.materials.size) { fallback }
        val materialTextures = arrayOfNulls<GltfMaterialTexture>(asset.materials.size)
        return try {
            for (index in identifiers.indices) {
                val texture = asset.textures[index]
                val image = asset.images.getOrNull(texture.imageIndex)?.let { NativeImageDecoder.decode(it.bytes) } ?: whiteImage()
                val dynamic = GltfDynamicTexture(identifiers[index].toString(), image, texture.sampler)
                textures[index] = dynamic
                manager.register(identifiers[index], dynamic)
                registered[index] = true
            }
            fallbackTexture = GltfDynamicTexture(fallback.toString(), whiteImage(), TextureSampler.DEFAULT)
            manager.register(fallback, fallbackTexture)
            fallbackRegistered = true
            if (IrisCompat.available()) {
                for (index in asset.materials.indices) {
                    val identifier = LibGltf.id("runtime/$resourceId/material_$index")
                    val texture = createMaterialTexture(asset, asset.materials[index], identifier)
                    materialIdentifiers[index] = identifier
                    materialTextures[index] = texture
                }
            } else {
                for (index in asset.materials.indices) {
                    val textureIndex = asset.materials[index].baseColorTexture?.textureIndex ?: -1
                    materialIdentifiers[index] = identifiers.getOrElse(textureIndex) { fallback }
                }
            }
            GltfTextureSet(
                identifiers,
                Array(textures.size) { requireNotNull(textures[it]) },
                materialIdentifiers,
                materialTextures,
                fallback
            )
        } catch (throwable: Throwable) {
            for (index in materialTextures.indices) {
                val texture = materialTextures[index] ?: continue
                texture.close()
                manager.release(materialIdentifiers[index])
            }
            for (index in textures.indices) {
                val texture = textures[index] ?: continue
                if (registered[index]) manager.release(identifiers[index]) else texture.close()
            }
            if (fallbackRegistered) manager.release(fallback) else fallbackTexture?.close()
            throw throwable
        }
    }

    private fun createMaterialTexture(
        asset: GltfAsset,
        material: GltfMaterial,
        identifier: Identifier
    ): GltfMaterialTexture {
        val manager = Minecraft.getInstance().textureManager
        val albedo = GltfDynamicTexture(
            identifier.toString(),
            image(asset, material.baseColorTexture) ?: whiteImage(),
            sampler(asset, material.baseColorTexture)
        )
        var normal: GltfDynamicTexture? = null
        var specular: GltfDynamicTexture? = null
        var materialTexture: GltfMaterialTexture? = null
        try {
            normal = GltfDynamicTexture(
                "$identifier/normal",
                LabPbrTextureEncoder.normal(asset, material),
                sampler(asset, material.normalTexture ?: material.occlusionTexture)
            )
            specular = GltfDynamicTexture(
                "$identifier/specular",
                LabPbrTextureEncoder.specular(asset, material),
                sampler(asset, material.metallicRoughnessTexture ?: material.specular?.texture)
            )
            materialTexture = GltfMaterialTexture(identifier, albedo, normal, specular)
            manager.register(identifier, albedo)
            return materialTexture
        } catch (throwable: Throwable) {
            if (materialTexture != null) {
                materialTexture.close()
            } else {
                normal?.close()
                specular?.close()
            }
            albedo.close()
            throw throwable
        }
    }

    private fun image(asset: GltfAsset, binding: TextureBinding?): NativeImage? {
        val texture = asset.textures.getOrNull(binding?.textureIndex ?: -1) ?: return null
        val bytes = asset.images.getOrNull(texture.imageIndex)?.bytes ?: return null
        return NativeImageDecoder.decode(bytes)
    }

    private fun sampler(asset: GltfAsset, binding: TextureBinding?): TextureSampler =
        asset.textures.getOrNull(binding?.textureIndex ?: -1)?.sampler ?: TextureSampler.DEFAULT

    private fun whiteImage(): NativeImage = NativeImage(1, 1, false).apply {
        setPixelABGR(0, 0, -1)
    }
}
