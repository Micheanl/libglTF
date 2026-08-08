package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationController
 *
 * <pre><code>
 * val animator: AnimationController = AnimationController(handle.asset, animation)
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import com.micheanl.libgltf.model.GltfAsset
import kotlin.math.abs

class AnimationController(
    private val asset: GltfAsset,
    val player: AnimationPlayer
) {
    var machine: AnimationStateMachine? = null
        private set

    var loopBlendSeconds: Float = 0.0f

    var currentState: Int = -1
        private set

    var stateTimeSeconds: Float = 0.0f
        private set

    var speed: Float
        get() = player.speed
        set(value) {
            player.speed = value
        }

    val playing: Boolean
        get() = player.playing

    val currentFrame: Int
        get() = activeSegment?.frameAtTime(player.timeSeconds) ?: 0

    private var activeSegment: AnimationSegment? = null
    private var pendingSegment: AnimationSegment? = null
    private var activeLooping = true
    private var paused = false
    private var floatParameters = FloatArray(0)
    private var intParameters = IntArray(0)
    private var booleanParameters = BooleanArray(0)

    fun segment(name: String, framesPerSecond: Float = 20.0f): AnimationSegment {
        val index = asset.animations.indexOfFirst { it.name == name }
        require(index >= 0) { "Unknown animation clip: $name" }
        return AnimationSegment.full(index, asset.animations[index], framesPerSecond)
    }

    fun segment(index: Int, framesPerSecond: Float = 20.0f): AnimationSegment {
        require(index in asset.animations.indices)
        return AnimationSegment.full(index, asset.animations[index], framesPerSecond)
    }

    fun play(segment: AnimationSegment, looping: Boolean = true, speed: Float = 1.0f) {
        validate(segment)
        activeSegment = segment
        pendingSegment = null
        activeLooping = looping
        paused = false
        player.play(segment.sourceClipIndex, false, speed)
        player.seek(if (speed >= 0.0f) segment.startSeconds else segment.endSeconds)
    }

    fun crossFadeTo(
        segment: AnimationSegment,
        durationSeconds: Float,
        looping: Boolean = true,
        speed: Float = 1.0f
    ) {
        validate(segment)
        require(durationSeconds >= 0.0f)
        if (durationSeconds == 0.0f || player.clipIndex < 0) {
            play(segment, looping, speed)
            return
        }
        val sourceEnd = activeSegment?.endSeconds ?: Float.MAX_VALUE
        pendingSegment = segment
        activeLooping = looping
        paused = false
        player.speed = speed
        player.looping = false
        player.crossFadeTo(
            segment.sourceClipIndex,
            durationSeconds,
            if (speed >= 0.0f) segment.startSeconds else segment.endSeconds,
            sourceEnd
        )
    }

    fun pause() {
        paused = true
        player.pause()
    }

    fun resume() {
        paused = false
        player.resume()
    }

    fun stop() {
        paused = false
        player.stop()
        activeSegment = null
        pendingSegment = null
        currentState = -1
        stateTimeSeconds = 0.0f
    }

    fun seek(seconds: Float) {
        val segment = activeSegment
        if (segment == null) player.seek(seconds) else player.seek(seconds.coerceIn(segment.startSeconds, segment.endSeconds))
    }

    fun seekFrame(frame: Int) {
        val segment = activeSegment ?: return
        player.seek(segment.timeAtFrame(frame))
    }

    fun stepFrames(frames: Int) {
        seekFrame(currentFrame + frames)
    }

    fun bind(machine: AnimationStateMachine, initialState: Int = machine.initialState) {
        require(initialState in machine.states.indices)
        this.machine = machine
        floatParameters = FloatArray(machine.floatParameterCount)
        intParameters = IntArray(machine.intParameterCount)
        booleanParameters = BooleanArray(machine.booleanParameterCount)
        enterState(initialState, 0.0f)
    }

    fun unbind() {
        machine = null
        currentState = -1
        stateTimeSeconds = 0.0f
        floatParameters = FloatArray(0)
        intParameters = IntArray(0)
        booleanParameters = BooleanArray(0)
    }

    fun set(parameter: AnimationParameter, value: Float) {
        require(parameter.type == AnimationParameterType.FLOAT)
        floatParameters[parameter.index] = value
    }

    fun set(parameter: AnimationParameter, value: Int) {
        require(parameter.type == AnimationParameterType.INT)
        intParameters[parameter.index] = value
    }

    fun set(parameter: AnimationParameter, value: Boolean) {
        require(parameter.type == AnimationParameterType.BOOLEAN)
        booleanParameters[parameter.index] = value
    }

    fun set(name: String, value: Float) {
        set(requireNotNull(machine).parameter(name), value)
    }

    fun set(name: String, value: Int) {
        set(requireNotNull(machine).parameter(name), value)
    }

    fun set(name: String, value: Boolean) {
        set(requireNotNull(machine).parameter(name), value)
    }

    fun evaluate(deltaSeconds: Float): AnimationPose {
        val stateMachine = machine
        if (stateMachine != null && currentState >= 0 && !paused) {
            stateTimeSeconds += abs(deltaSeconds)
            val transition = selectTransition(stateMachine)
            if (transition != null) enterState(transition.toState, transition.fadeSeconds)
        }
        val pose = player.evaluate(deltaSeconds)
        val pending = pendingSegment
        if (pending != null && player.clipIndex == pending.sourceClipIndex) {
            activeSegment = pending
            pendingSegment = null
        }
        if (!paused) enforceSegment()
        return pose
    }

    private fun selectTransition(machine: AnimationStateMachine): AnimationTransition? {
        val candidates = machine.transitionsByState[currentState]
        for (candidate in candidates) {
            val transition = machine.transitions[candidate]
            if (transition.toState == currentState) continue
            if (stateTimeSeconds < transition.minimumStateSeconds) continue
            if (transition.exitTime >= 0.0f && normalizedTime() < transition.exitTime) continue
            var matches = true
            for (condition in transition.conditions) {
                if (!condition.matches(floatParameters, intParameters, booleanParameters)) {
                    matches = false
                    break
                }
            }
            if (matches) return transition
        }
        return null
    }

    private fun enterState(index: Int, fadeSeconds: Float) {
        val state = requireNotNull(machine).states[index]
        currentState = index
        stateTimeSeconds = 0.0f
        if (fadeSeconds > 0.0f) {
            crossFadeTo(state.segment, fadeSeconds, state.looping, state.speed)
        } else {
            play(state.segment, state.looping, state.speed)
        }
    }

    private fun enforceSegment() {
        if (pendingSegment != null || player.fading) return
        val segment = activeSegment ?: return
        if (player.clipIndex != segment.sourceClipIndex) return
        val duration = segment.durationSeconds
        if (player.speed >= 0.0f && player.timeSeconds >= segment.endSeconds) {
            if (activeLooping && duration > 0.0f) {
                if (loopBlendSeconds > 0.0f) {
                    crossFadeTo(segment, loopBlendSeconds, activeLooping, player.speed)
                    return
                }
                val overflow = (player.timeSeconds - segment.startSeconds) % duration
                player.seek(segment.startSeconds + overflow)
                player.resume()
            } else {
                player.seek(segment.endSeconds)
                player.pause()
            }
        } else if (player.speed < 0.0f && player.timeSeconds <= segment.startSeconds) {
            if (activeLooping && duration > 0.0f) {
                if (loopBlendSeconds > 0.0f) {
                    crossFadeTo(segment, loopBlendSeconds, activeLooping, player.speed)
                    return
                }
                val overflow = (segment.endSeconds - player.timeSeconds) % duration
                player.seek(segment.endSeconds - overflow)
                player.resume()
            } else {
                player.seek(segment.startSeconds)
                player.pause()
            }
        }
    }

    private fun normalizedTime(): Float {
        val segment = activeSegment ?: return 0.0f
        if (segment.durationSeconds <= 0.0f) return 1.0f
        return ((player.timeSeconds - segment.startSeconds) / segment.durationSeconds).coerceIn(0.0f, 1.0f)
    }

    private fun validate(segment: AnimationSegment) {
        require(segment.sourceClipIndex in asset.animations.indices)
        require(segment.endSeconds <= asset.animations[segment.sourceClipIndex].durationSeconds)
    }
}
