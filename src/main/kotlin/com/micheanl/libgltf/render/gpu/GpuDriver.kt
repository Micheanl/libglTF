package com.micheanl.libgltf.render.gpu

interface GpuDriver : AutoCloseable {
    val meshSupported: Boolean
}
