package com.micheanl.libgltf.animation




/**
 * libgltf · AnimationStateMachine
 *
 * ```
 * var machine: AnimationStateMachine? = null
 * ```
 *
 * 动画状态机
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class AnimationStateMachine(
    val parameters: Array<AnimationParameter>,
    val states: Array<AnimationState>,
    val transitions: Array<AnimationTransition>,
    val initialState: Int,
    internal val floatParameterCount: Int,
    internal val intParameterCount: Int,
    internal val booleanParameterCount: Int
) {
    internal val transitionsByState: Array<IntArray> = Array(states.size) { stateIndex ->
        transitions.indices
            .filter { transitions[it].fromState == -1 || transitions[it].fromState == stateIndex }
            .sortedWith(compareByDescending<Int> { transitions[it].priority }.thenBy { it })
            .toIntArray()
    }

    init {
        require(states.isNotEmpty())
        require(initialState in states.indices)
        for (transition in transitions) {
            require(transition.fromState == -1 || transition.fromState in states.indices)
            require(transition.toState in states.indices)
        }
    }

    fun parameter(name: String): AnimationParameter =
        parameters.firstOrNull { it.name == name } ?: error("Unknown animation parameter: $name")

    fun state(name: String): Int = states.indexOfFirst { it.name == name }.also {
        require(it >= 0) { "Unknown animation state: $name" }
    }
}
