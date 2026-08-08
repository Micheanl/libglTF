package com.micheanl.libgltf.asset

/**
 * libgltf · GltfLoadResult
 *
 * ```
 * fun loadAsync(path: Path, lodPolicy: LodPolicy = LodPolicy.DEFAULT): CompletableFuture<GltfLoadResult>
 * ```
 *
 * 加载结果：成功或失败
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

sealed interface GltfLoadResult
