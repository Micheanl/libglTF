package com.micheanl.libgltf.render.vulkan




/**
 * libgltf · VulkanUsage
 *
 * ```
 * if ((usage & VulkanUsage.STORAGE) != 0) {
 * ```
 *
 * Vulkan 缓冲用途常量
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

object VulkanUsage {
    const val STORAGE: Int = 1 shl 10
}
