package com.micheanl.libgltf.render.gpu

/**
 * libgltf · GpuDriver
 *
 * ```
 * private var gpuDrivenDriver: GpuDriver? = null
 * ```
 *
 * GPU 驱动抽象：mesh 与间接绘制
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

interface GpuDriver : AutoCloseable {
    val meshSupported: Boolean
}
