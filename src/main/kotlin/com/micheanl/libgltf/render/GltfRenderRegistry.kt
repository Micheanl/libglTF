package com.micheanl.libgltf.render

import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.api.GltfInstanceId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


/**
 * libgltf · GltfRenderRegistry
 *
 * ```
 * override fun register(instance: GltfInstance): GltfInstanceId = GltfRenderRegistry.register(instance)
 * ```
 *
 * 渲染类型注册表
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object GltfRenderRegistry {
    private val nextId = AtomicLong(1L)
    private val instancesById = ConcurrentHashMap<Long, GltfInstance>()

    @Volatile
    private var snapshot: Array<GltfInstance> = emptyArray()

    fun register(instance: GltfInstance): GltfInstanceId {
        require(!instance.handle.isClosed)
        val id = nextId.getAndIncrement()
        instancesById[id] = instance
        snapshot = instancesById.values.toTypedArray()
        return GltfInstanceId(id)
    }

    fun unregister(id: GltfInstanceId): Boolean {
        val removed = instancesById.remove(id.value) != null
        if (removed) snapshot = instancesById.values.toTypedArray()
        return removed
    }

    fun instances(): Array<GltfInstance> = snapshot

    fun removeByResource(resourceId: Long) {
        var changed = false
        for ((id, instance) in instancesById) {
            if (instance.handle.resourceId == resourceId && instancesById.remove(id, instance)) changed = true
        }
        if (changed) snapshot = instancesById.values.toTypedArray()
    }
}
