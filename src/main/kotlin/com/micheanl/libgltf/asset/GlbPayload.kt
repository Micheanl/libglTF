package com.micheanl.libgltf.asset

/**
 * libgltf · GlbPayload
 *
 * ```
 * return GlbPayload(json, binary)
 * ```
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import java.nio.ByteBuffer

data class GlbPayload(val json: ByteArray, val binary: ByteBuffer?)
