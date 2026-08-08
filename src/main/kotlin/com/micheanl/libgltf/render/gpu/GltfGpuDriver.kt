package com.micheanl.libgltf.render.gpu

interface GltfGpuDriver : AutoCloseable {
    val meshSupported: Boolean
}
