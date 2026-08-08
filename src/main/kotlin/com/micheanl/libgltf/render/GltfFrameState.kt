package com.micheanl.libgltf.render

/**
 * libgltf · GltfFrameState
 *
 * 当前帧实例状态
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import com.micheanl.libgltf.api.GltfInstance

object GltfFrameState {
    @Volatile
    private var instances: Array<GltfInstance> = emptyArray()

    fun capture() {
        instances = GltfRenderRegistry.instances()
    }

    fun instances(): Array<GltfInstance> = instances
}
