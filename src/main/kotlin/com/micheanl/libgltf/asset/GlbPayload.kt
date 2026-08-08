package com.micheanl.libgltf.asset

/**
 * libgltf · GlbPayload
 *
 * <pre><code>
 * return GlbPayload(json, binary)
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import java.nio.ByteBuffer

data class GlbPayload(val json: ByteArray, val binary: ByteBuffer?)
