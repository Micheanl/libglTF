package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationParameter
 *
 * <pre><code>
 * return AnimationParameter(name, index, type).also(parameters::add)
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


data class AnimationParameter(
    val name: String,
    val index: Int,
    val type: AnimationParameterType
)
