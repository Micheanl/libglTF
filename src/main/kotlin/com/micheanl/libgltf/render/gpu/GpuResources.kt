package com.micheanl.libgltf.render.gpu

/**
 * libgltf · GpuResources
 *
 * <pre><code>
 * resources = GpuResources(id, asset)
 * </code></pre>
 *
 * 资源对应的 GPU 数据
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.model.GltfAsset
import com.micheanl.libgltf.render.GpuBackendType
import com.micheanl.libgltf.render.vulkan.RenderConfig
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.logging.LogUtils

class GpuResources(
    private val resourceId: Long,
    private val asset: GltfAsset
) : AutoCloseable {
    private val primitives: Array<Array<GpuMesh?>> = Array(asset.meshes.size) { meshIndex ->
        arrayOfNulls(asset.meshes[meshIndex].primitives.size)
    }
    private val failed: Array<BooleanArray> = Array(asset.meshes.size) { meshIndex ->
        BooleanArray(asset.meshes[meshIndex].primitives.size)
    }

    fun primitive(meshIndex: Int, primitiveIndex: Int): GpuMesh? {
        if (failed[meshIndex][primitiveIndex]) return null
        var primitive = primitives[meshIndex][primitiveIndex]
        if (primitive == null) {
            try {
                primitive = GpuMesh.create(
                    RenderSystem.getDevice(),
                    "libgltf $resourceId mesh $meshIndex primitive $primitiveIndex",
                    asset.meshes[meshIndex].primitives[primitiveIndex],
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

    fun failed(meshIndex: Int, primitiveIndex: Int): Boolean = failed[meshIndex][primitiveIndex]

    fun disable(meshIndex: Int, primitiveIndex: Int) {
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
