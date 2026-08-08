package com.micheanl.libgltf.api

/**
 * libgltf · GltfHandle
 *
 * ```
 * return GltfHandle(asset, id)
 * ```
 *
 * 已加载 glTF 资源的句柄
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.model.GltfAsset
import com.micheanl.libgltf.render.GltfRenderSystem
import java.util.concurrent.atomic.AtomicBoolean

class GltfHandle internal constructor(
    val asset: GltfAsset,
    internal val resourceId: Long
) : AutoCloseable {
    private val closed = AtomicBoolean()

    val isClosed: Boolean
        get() = closed.get()

    override fun close() {
        if (closed.compareAndSet(false, true)) GltfRenderSystem.release(resourceId)
    }
}
