package com.micheanl.libgltf.render

import com.micheanl.libgltf.api.GltfInstance




/**
 * libgltf · GltfFrameState
 *
 * ```
 * LevelExtractionEvents.END_EXTRACTION.register { GltfFrameState.capture() }
 * ```
 *
 * 当前帧实例状态
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object GltfFrameState {
    @Volatile
    private var instances: Array<GltfInstance> = emptyArray()

    fun capture() {
        instances = GltfRenderRegistry.instances()
    }

    fun instances(): Array<GltfInstance> = instances
}
