package com.micheanl.libgltf.render

import com.micheanl.libgltf.model.GltfAsset
import com.micheanl.libgltf.render.gpu.GpuResources
import com.micheanl.libgltf.render.texture.GltfTextureFactory
import com.micheanl.libgltf.render.texture.GltfTextureSet




/**
 * libgltf · GltfRenderAsset
 *
 * ```
 * fun renderType(resource: GltfRenderAsset, textures: GltfTextureSet): RenderType {
 * ```
 *
 * 渲染资源封装
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GltfRenderAsset(
    val id: Long,
    val asset: GltfAsset
) : AutoCloseable {
    @Volatile
    private var textureSet: GltfTextureSet? = null
    private var gpuResources: GpuResources? = null

    fun textures(): GltfTextureSet {
        var textures = textureSet
        if (textures == null) {
            textures = GltfTextureFactory.create(asset, id)
            textureSet = textures
        }
        return textures
    }

    fun gpu(): GpuResources {
        var resources = gpuResources
        if (resources == null) {
            resources = GpuResources(id, asset)
            gpuResources = resources
        }
        return resources
    }

    override fun close() {
        gpuResources?.close()
        gpuResources = null
        textureSet?.close()
        textureSet = null
    }
}
