package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationStateMachineBuilder
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

import com.micheanl.libgltf.model.GltfAsset

class AnimationStateMachineBuilder(private val asset: GltfAsset) {
    private val parameters = ArrayList<AnimationParameter>()
    private val states = ArrayList<AnimationState>()
    private val transitions = ArrayList<AnimationTransition>()
    private var floatCount = 0
    private var intCount = 0
    private var booleanCount = 0
    private var initialState = 0

    fun floatParameter(name: String): AnimationParameter = parameter(name, AnimationParameterType.FLOAT)

    fun intParameter(name: String): AnimationParameter = parameter(name, AnimationParameterType.INT)

    fun booleanParameter(name: String): AnimationParameter = parameter(name, AnimationParameterType.BOOLEAN)

    fun clip(name: String, framesPerSecond: Float = 20.0f): AnimationSegment {
        val index = asset.animations.indexOfFirst { it.name == name }
        require(index >= 0) { "Unknown animation clip: $name" }
        return AnimationSegment.full(index, asset.animations[index], framesPerSecond)
    }

    fun clip(index: Int, framesPerSecond: Float = 20.0f): AnimationSegment {
        require(index in asset.animations.indices)
        return AnimationSegment.full(index, asset.animations[index], framesPerSecond)
    }

    fun frames(
        name: String,
        sourceClipIndex: Int,
        firstFrame: Int,
        lastFrame: Int,
        framesPerSecond: Float
    ): AnimationSegment {
        require(sourceClipIndex in asset.animations.indices)
        val segment = AnimationSegment.frames(name, sourceClipIndex, firstFrame, lastFrame, framesPerSecond)
        require(segment.endSeconds <= asset.animations[sourceClipIndex].durationSeconds)
        return segment
    }

    fun state(
        name: String,
        segment: AnimationSegment,
        looping: Boolean = true,
        speed: Float = 1.0f,
        initial: Boolean = false
    ): Int {
        require(states.none { it.name == name })
        validate(segment)
        val index = states.size
        states.add(AnimationState(name, segment, looping, speed))
        if (initial) initialState = index
        return index
    }

    fun transition(
        fromState: Int,
        toState: Int,
        fadeSeconds: Float = 0.0f,
        minimumStateSeconds: Float = 0.0f,
        exitTime: Float = -1.0f,
        priority: Int = 0,
        vararg conditions: AnimationCondition
    ): AnimationStateMachineBuilder {
        transitions.add(
            AnimationTransition(
                fromState,
                toState,
                conditions.copyOf(),
                fadeSeconds,
                minimumStateSeconds,
                exitTime,
                priority
            )
        )
        return this
    }

    fun transition(
        fromState: String,
        toState: String,
        fadeSeconds: Float = 0.0f,
        minimumStateSeconds: Float = 0.0f,
        exitTime: Float = -1.0f,
        priority: Int = 0,
        vararg conditions: AnimationCondition
    ): AnimationStateMachineBuilder = transition(
        stateIndex(fromState),
        stateIndex(toState),
        fadeSeconds,
        minimumStateSeconds,
        exitTime,
        priority,
        *conditions
    )

    fun build(): AnimationStateMachine = AnimationStateMachine(
        parameters.toTypedArray(),
        states.toTypedArray(),
        transitions.toTypedArray(),
        initialState,
        floatCount,
        intCount,
        booleanCount
    )

    private fun parameter(name: String, type: AnimationParameterType): AnimationParameter {
        require(parameters.none { it.name == name })
        val index = when (type) {
            AnimationParameterType.FLOAT -> floatCount++
            AnimationParameterType.INT -> intCount++
            AnimationParameterType.BOOLEAN -> booleanCount++
        }
        return AnimationParameter(name, index, type).also(parameters::add)
    }

    private fun stateIndex(name: String): Int = states.indexOfFirst { it.name == name }.also {
        require(it >= 0) { "Unknown animation state: $name" }
    }

    private fun validate(segment: AnimationSegment) {
        require(segment.sourceClipIndex in asset.animations.indices)
        require(segment.endSeconds <= asset.animations[segment.sourceClipIndex].durationSeconds)
    }
}
