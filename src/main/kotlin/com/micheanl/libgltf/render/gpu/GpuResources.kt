package com.micheanl.libgltf.render.gpu

import com.micheanl.libgltf.model.GltfAsset
import com.micheanl.libgltf.render.GltfMeshSource
import com.micheanl.renderapi.MeshResources
import com.micheanl.renderapi.gpu.GpuBackend
import com.micheanl.renderapi.gpu.GpuMesh
import com.micheanl.renderapi.vulkan.RenderConfig
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.logging.LogUtils

/**
 * libgltf · GpuResources
 *
 * ```
 * private var gpuResources: GpuResources? = null
 * ```
 *
 * 资源对应的 GPU 数据
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class GpuResources(
    private val resourceId: Long,
    private val asset: GltfAsset
) : AutoCloseable, MeshResources {
    private val primitives: Array<Array<GpuMesh?>> = Array(asset.meshes.size) { meshIndex ->
        arrayOfNulls(asset.meshes[meshIndex].primitives.size)
    }
    private val failed: Array<BooleanArray> = Array(asset.meshes.size) { meshIndex ->
        BooleanArray(asset.meshes[meshIndex].primitives.size)
    }

    override fun primitive(meshIndex: Int, primitiveIndex: Int): GpuMesh? {
        if (failed[meshIndex][primitiveIndex]) return null
        var primitive = primitives[meshIndex][primitiveIndex]
        if (primitive == null) {
            try {
                primitive = GpuMesh.create(
                    RenderSystem.getDevice(),
                    "libgltf $resourceId mesh $meshIndex primitive $primitiveIndex",
                    GltfMeshSource(asset.meshes[meshIndex].primitives[primitiveIndex]),
                    RenderConfig.enabled && GpuBackend.meshletBuilding()
                )
                primitives[meshIndex][primitiveIndex] = primitive
            } catch (error: RuntimeException) {
                disable(meshIndex, primitiveIndex)
                LOGGER.error("libgltf GPU primitive creation failed for resource {} mesh {} primitive {}", resourceId, meshIndex, primitiveIndex, error)
                return null
            }
        }
        return primitive
    }

    override fun failed(meshIndex: Int, primitiveIndex: Int): Boolean = failed[meshIndex][primitiveIndex]

    override fun disable(meshIndex: Int, primitiveIndex: Int) {
        failed[meshIndex][primitiveIndex] = true
        primitives[meshIndex][primitiveIndex]?.close()
        primitives[meshIndex][primitiveIndex] = null
    }

    override fun close() {
        for (mesh in primitives) {
            for (primitive in mesh) primitive?.close()
        }
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
    }
}
