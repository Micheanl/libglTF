package com.micheanl.libgltf.asset

import java.nio.ByteBuffer


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

data class GlbPayload(val json: ByteArray, val binary: ByteBuffer?)
