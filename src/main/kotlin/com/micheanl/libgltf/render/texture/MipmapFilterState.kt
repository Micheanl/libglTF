package com.micheanl.libgltf.render.texture

/**
 * libgltf · MipmapFilterState
 *
 * ```
 * return MipmapFilterState.nearest() ? VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST : original;
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.material.TextureFilter

object MipmapFilterState {
    private val active = ThreadLocal.withInitial { TextureFilter.LINEAR }

    @JvmStatic
    fun nearest(): Boolean = active.get() == TextureFilter.NEAREST

    fun <T> create(filter: TextureFilter, factory: () -> T): T {
        val previous = active.get()
        active.set(filter)
        return try {
            factory()
        } finally {
            active.set(previous)
        }
    }
}
