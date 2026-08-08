package com.micheanl.libgltf.asset

import java.nio.ByteBuffer




/**
 * libgltf · GlbPayload
 *
 * ```
 * fun read(source: ByteBuffer): GlbPayload {
 * ```
 *
 * GLB 二进制块载荷
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class GlbPayload(val json: ByteArray, val binary: ByteBuffer?)
