package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationSegment
 *
 * <pre><code>
 * return AnimationSegment.full(index, asset.animations[index], framesPerSecond)
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


import kotlin.math.floor

data class AnimationSegment(
    val name: String,
    val sourceClipIndex: Int,
    val startSeconds: Float,
    val endSeconds: Float,
    val framesPerSecond: Float = 20.0f
) {
    init {
        require(sourceClipIndex >= 0)
        require(startSeconds >= 0.0f)
        require(endSeconds >= startSeconds)
        require(framesPerSecond > 0.0f)
    }

    val durationSeconds: Float
        get() = endSeconds - startSeconds

    val frameCount: Int
        get() = floor(durationSeconds * framesPerSecond).toInt() + 1

    fun timeAtFrame(frame: Int): Float =
        (startSeconds + frame.coerceIn(0, frameCount - 1) / framesPerSecond).coerceAtMost(endSeconds)

    fun frameAtTime(seconds: Float): Int =
        floor((seconds.coerceIn(startSeconds, endSeconds) - startSeconds) * framesPerSecond).toInt()

    companion object {
        fun full(index: Int, clip: AnimationClip, framesPerSecond: Float = 20.0f): AnimationSegment =
            AnimationSegment(clip.name, index, 0.0f, clip.durationSeconds, framesPerSecond)

        fun frames(
            name: String,
            sourceClipIndex: Int,
            firstFrame: Int,
            lastFrame: Int,
            framesPerSecond: Float
        ): AnimationSegment {
            require(firstFrame >= 0)
            require(lastFrame >= firstFrame)
            return AnimationSegment(
                name,
                sourceClipIndex,
                firstFrame / framesPerSecond,
                lastFrame / framesPerSecond,
                framesPerSecond
            )
        }
    }
}
