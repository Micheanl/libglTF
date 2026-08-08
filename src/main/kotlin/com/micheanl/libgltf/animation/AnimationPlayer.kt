package com.micheanl.libgltf.animation

import com.micheanl.libgltf.model.GltfAsset
import com.micheanl.libgltf.material.MaterialFactorAnimationState
import com.micheanl.libgltf.material.MaterialUvAnimationState
import org.joml.Quaternionf
import kotlin.math.abs




/**
 * libgltf · AnimationPlayer
 *
 * ```
 * val player: AnimationPlayer
 * ```
 *
 * 动画播放与姿态计算
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

class AnimationPlayer(private val asset: GltfAsset) {
    val pose: AnimationPose = AnimationPose(asset.nodes.size, asset.totalMorphWeights, asset.materials.size)

    var revision: Long = 0L
        private set

    var speed: Float = 1.0f
    var looping: Boolean = true
    var playing: Boolean = false
        private set
    var timeSeconds: Float = 0.0f
        private set
    var clipIndex: Int = -1
        private set

    private var fadeClipIndex: Int = -1
    private var fadeTimeSeconds: Float = 0.0f
    private var fadeDurationSeconds: Float = 0.0f
    private var fadeElapsedSeconds: Float = 0.0f
    private var fadeSourceEndSeconds: Float = Float.MAX_VALUE
    private var dirty: Boolean = true

    private val restTranslation = FloatArray(asset.nodes.size * 3)
    private val restRotation = FloatArray(asset.nodes.size * 4)
    private val restScale = FloatArray(asset.nodes.size * 3)
    private val restMorph = FloatArray(asset.totalMorphWeights)
    private val restMaterialUv = MaterialUvAnimationState(asset.materials.size)
    private val restMaterialFactor = MaterialFactorAnimationState(asset.materials.size)
    private val primaryTranslation = FloatArray(restTranslation.size)
    private val primaryRotation = FloatArray(restRotation.size)
    private val primaryScale = FloatArray(restScale.size)
    private val primaryMorph = FloatArray(restMorph.size)
    private val primaryMaterialUv = MaterialUvAnimationState(asset.materials.size)
    private val primaryMaterialFactor = MaterialFactorAnimationState(asset.materials.size)
    private val secondaryTranslation = FloatArray(restTranslation.size)
    private val secondaryRotation = FloatArray(restRotation.size)
    private val secondaryScale = FloatArray(restScale.size)
    private val secondaryMorph = FloatArray(restMorph.size)
    private val secondaryMaterialUv = MaterialUvAnimationState(asset.materials.size)
    private val secondaryMaterialFactor = MaterialFactorAnimationState(asset.materials.size)
    private val animatedNodes = BooleanArray(asset.nodes.size)
    private val secondaryAnimatedNodes = BooleanArray(asset.nodes.size)
    private val sample = FloatArray(maxComponentCount())
    private val primaryKeys = IntArray(maxChannelCount()) { -1 }
    private val secondaryKeys = IntArray(primaryKeys.size) { -1 }
    private val quaternionA = Quaternionf()
    private val quaternionB = Quaternionf()

    init {
        initializeRestPose()
        evaluate(0.0f)
    }

    val fading: Boolean
        get() = fadeClipIndex >= 0

    fun play(index: Int, loop: Boolean = true, playbackSpeed: Float = 1.0f) {
        require(index in asset.animations.indices)
        clipIndex = index
        looping = loop
        speed = playbackSpeed
        timeSeconds = 0.0f
        fadeClipIndex = -1
        fadeSourceEndSeconds = Float.MAX_VALUE
        playing = true
        dirty = true
        resetKeys()
    }

    fun crossFadeTo(
        index: Int,
        durationSeconds: Float,
        startAtSeconds: Float = 0.0f,
        sourceEndSeconds: Float = Float.MAX_VALUE
    ) {
        require(index in asset.animations.indices)
        require(durationSeconds > 0.0f)
        if (clipIndex < 0) {
            play(index, looping, speed)
            return
        }
        fadeClipIndex = index
        fadeTimeSeconds = startAtSeconds.coerceIn(0.0f, asset.animations[index].durationSeconds)
        fadeSourceEndSeconds = sourceEndSeconds
        fadeDurationSeconds = durationSeconds
        fadeElapsedSeconds = 0.0f
        playing = true
        dirty = true
        secondaryKeys.fill(-1)
    }

    fun pause() {
        playing = false
    }

    fun resume() {
        if (clipIndex >= 0) playing = true
    }

    fun stop() {
        clipIndex = -1
        fadeClipIndex = -1
        timeSeconds = 0.0f
        playing = false
        dirty = true
        resetKeys()
        evaluate(0.0f)
    }

    fun seek(seconds: Float) {
        timeSeconds = normalizeTime(clipIndex, seconds)
        dirty = true
        primaryKeys.fill(-1)
        evaluate(0.0f)
    }

    fun evaluate(deltaSeconds: Float): AnimationPose {
        var changed = dirty
        if (playing && clipIndex >= 0) {
            val previousTime = timeSeconds
            timeSeconds = normalizeTime(clipIndex, previousTime + deltaSeconds * speed)
            if (fadeClipIndex >= 0 && timeSeconds > fadeSourceEndSeconds) timeSeconds = fadeSourceEndSeconds
            if (wrapped(previousTime, timeSeconds)) primaryKeys.fill(-1)
            if (timeSeconds != previousTime) changed = true
            if (!looping) {
                val duration = asset.animations[clipIndex].durationSeconds
                if (speed >= 0.0f && timeSeconds >= duration || speed < 0.0f && timeSeconds <= 0.0f) playing = false
            }
            if (fadeClipIndex >= 0) {
                val previousFadeTime = fadeTimeSeconds
                fadeTimeSeconds = normalizeTime(fadeClipIndex, previousFadeTime + deltaSeconds * speed)
                if (wrapped(previousFadeTime, fadeTimeSeconds)) secondaryKeys.fill(-1)
                fadeElapsedSeconds += abs(deltaSeconds)
                if (fadeTimeSeconds != previousFadeTime || deltaSeconds != 0.0f) changed = true
                if (fadeElapsedSeconds >= fadeDurationSeconds) {
                    clipIndex = fadeClipIndex
                    timeSeconds = fadeTimeSeconds
                    fadeClipIndex = -1
                    fadeSourceEndSeconds = Float.MAX_VALUE
                    secondaryKeys.copyInto(primaryKeys)
                    secondaryKeys.fill(-1)
                }
            }
        }
        if (!changed) return pose
        reset(
            primaryTranslation,
            primaryRotation,
            primaryScale,
            primaryMorph,
            animatedNodes,
            primaryMaterialUv,
            primaryMaterialFactor
        )
        if (clipIndex >= 0) {
            sample(
                asset.animations[clipIndex],
                timeSeconds,
                primaryTranslation,
                primaryRotation,
                primaryScale,
                primaryMorph,
                animatedNodes,
                primaryKeys,
                primaryMaterialUv,
                primaryMaterialFactor
            )
        }
        if (fadeClipIndex >= 0) {
            reset(
                secondaryTranslation,
                secondaryRotation,
                secondaryScale,
                secondaryMorph,
                secondaryAnimatedNodes,
                secondaryMaterialUv,
                secondaryMaterialFactor
            )
            sample(
                asset.animations[fadeClipIndex],
                fadeTimeSeconds,
                secondaryTranslation,
                secondaryRotation,
                secondaryScale,
                secondaryMorph,
                secondaryAnimatedNodes,
                secondaryKeys,
                secondaryMaterialUv,
                secondaryMaterialFactor
            )
            buildPose((fadeElapsedSeconds / fadeDurationSeconds).coerceIn(0.0f, 1.0f))
        } else {
            buildPose(0.0f)
        }
        dirty = false
        revision++
        return pose
    }

    private fun initializeRestPose() {
        restMaterialUv.reset()
        restMaterialFactor.reset()
        for (nodeIndex in asset.nodes.indices) {
            val node = asset.nodes[nodeIndex]
            node.translation.copyInto(restTranslation, nodeIndex * 3, 0, 3)
            node.rotation.copyInto(restRotation, nodeIndex * 4, 0, 4)
            node.scale.copyInto(restScale, nodeIndex * 3, 0, 3)
            val meshIndex = node.meshIndex
            if (meshIndex >= 0) {
                val offset = asset.morphOffsets[nodeIndex]
                val mesh = asset.meshes[meshIndex]
                val weights = if (node.weights.isNotEmpty()) node.weights else mesh.weights
                weights.copyInto(restMorph, offset, 0, weights.size.coerceAtMost(restMorph.size - offset))
            }
        }
        for (materialIndex in asset.materials.indices) {
            val factor = asset.materials[materialIndex].baseColorFactor
            val base = materialIndex * 4
            restMaterialFactor.baseColorFactor[base] = factor.getOrElse(0) { 1.0f }
            restMaterialFactor.baseColorFactor[base + 1] = factor.getOrElse(1) { 1.0f }
            restMaterialFactor.baseColorFactor[base + 2] = factor.getOrElse(2) { 1.0f }
            restMaterialFactor.baseColorFactor[base + 3] = factor.getOrElse(3) { 1.0f }
            val binding = asset.materials[materialIndex].baseColorTexture ?: continue
            restMaterialUv.offsetX[materialIndex] = binding.offsetX
            restMaterialUv.offsetY[materialIndex] = binding.offsetY
            restMaterialUv.rotation[materialIndex] = binding.rotation
            restMaterialUv.scaleX[materialIndex] = binding.scaleX
            restMaterialUv.scaleY[materialIndex] = binding.scaleY
        }
    }

    private fun reset(
        translation: FloatArray,
        rotation: FloatArray,
        scale: FloatArray,
        morph: FloatArray,
        animated: BooleanArray,
        materialUv: MaterialUvAnimationState,
        materialFactor: MaterialFactorAnimationState
    ) {
        restTranslation.copyInto(translation)
        restRotation.copyInto(rotation)
        restScale.copyInto(scale)
        restMorph.copyInto(morph)
        animated.fill(false)
        materialUv.copyFrom(restMaterialUv)
        materialFactor.copyFrom(restMaterialFactor)
    }

    private fun sample(
        clip: AnimationClip,
        time: Float,
        translation: FloatArray,
        rotation: FloatArray,
        scale: FloatArray,
        morph: FloatArray,
        animated: BooleanArray,
        keys: IntArray,
        materialUv: MaterialUvAnimationState,
        materialFactor: MaterialFactorAnimationState
    ) {
        for (channelIndex in clip.channels.indices) {
            val channel = clip.channels[channelIndex]
            keys[channelIndex] = sampleChannel(channel, time, sample, keys[channelIndex])
            if (channel.path == AnimationPath.MATERIAL_UV) {
                val materialIndex = channel.materialIndex
                if (materialIndex in asset.materials.indices && channel.textureSlot == 0) {
                    when (channel.textureProperty) {
                        0 -> {
                            materialUv.offsetX[materialIndex] = sample[0]
                            materialUv.offsetY[materialIndex] = sample.getOrElse(1) { 0.0f }
                        }
                        1 -> materialUv.rotation[materialIndex] = sample[0]
                        2 -> {
                            materialUv.scaleX[materialIndex] = sample[0]
                            materialUv.scaleY[materialIndex] = sample.getOrElse(1) { 1.0f }
                        }
                    }
                    materialUv.animated[materialIndex] = true
                }
                continue
            }
            if (channel.path == AnimationPath.MATERIAL_FACTOR) {
                val materialIndex = channel.materialIndex
                if (materialIndex in asset.materials.indices) {
                    sample.copyInto(
                        materialFactor.baseColorFactor,
                        materialIndex * 4,
                        0,
                        channel.componentCount.coerceAtMost(4)
                    )
                    materialFactor.animated[materialIndex] = true
                }
                continue
            }
            val node = channel.nodeIndex
            animated[node] = true
            when (channel.path) {
                AnimationPath.TRANSLATION -> sample.copyInto(translation, node * 3, 0, 3)
                AnimationPath.ROTATION -> {
                    quaternionA.set(sample[0], sample[1], sample[2], sample[3]).normalize()
                    rotation[node * 4] = quaternionA.x
                    rotation[node * 4 + 1] = quaternionA.y
                    rotation[node * 4 + 2] = quaternionA.z
                    rotation[node * 4 + 3] = quaternionA.w
                }
                AnimationPath.SCALE -> sample.copyInto(scale, node * 3, 0, 3)
                AnimationPath.WEIGHTS -> {
                    val offset = asset.morphOffsets[node]
                    sample.copyInto(morph, offset, 0, channel.componentCount.coerceAtMost(morph.size - offset))
                }
            }
        }
    }

    private fun sampleChannel(channel: AnimationChannel, time: Float, output: FloatArray, cursor: Int): Int {
        val times = channel.times
        if (times.isEmpty()) {
            output.fill(0.0f, 0, channel.componentCount)
            return -1
        }
        if (times.size == 1 || time <= times[0]) {
            copyKey(channel, 0, output)
            return 0
        }
        val last = times.lastIndex
        if (time >= times[last]) {
            copyKey(channel, last, output)
            return last - 1
        }
        var low = cursor
        if (low < 0 || low >= last) {
            low = findKey(times, time)
        } else {
            while (low > 0 && time < times[low]) low--
            while (low + 1 < last && time >= times[low + 1]) low++
        }
        val high = low + 1
        val duration = times[high] - times[low]
        val factor = if (duration > 0.0f) (time - times[low]) / duration else 0.0f
        when (channel.interpolation) {
            Interpolation.STEP -> copyKey(channel, low, output)
            Interpolation.LINEAR -> interpolateLinear(channel, low, high, factor, output)
            Interpolation.CUBIC_SPLINE -> interpolateCubic(channel, low, high, factor, duration, output)
        }
        return low
    }

    private fun findKey(times: FloatArray, time: Float): Int {
        var low = 0
        var high = times.lastIndex
        while (low + 1 < high) {
            val middle = (low + high) ushr 1
            if (times[middle] <= time) low = middle else high = middle
        }
        return low
    }

    private fun copyKey(channel: AnimationChannel, key: Int, output: FloatArray) {
        val components = channel.componentCount
        val multiplier = if (channel.interpolation == Interpolation.CUBIC_SPLINE) 3 else 1
        val base = (key * multiplier + if (multiplier == 3) 1 else 0) * components
        channel.values.copyInto(output, 0, base, base + components)
    }

    private fun interpolateLinear(channel: AnimationChannel, low: Int, high: Int, factor: Float, output: FloatArray) {
        val components = channel.componentCount
        val lowBase = low * components
        val highBase = high * components
        if (channel.path == AnimationPath.ROTATION && components == 4) {
            quaternionA.set(
                channel.values[lowBase],
                channel.values[lowBase + 1],
                channel.values[lowBase + 2],
                channel.values[lowBase + 3]
            ).normalize()
            quaternionB.set(
                channel.values[highBase],
                channel.values[highBase + 1],
                channel.values[highBase + 2],
                channel.values[highBase + 3]
            ).normalize()
            quaternionA.slerp(quaternionB, factor)
            output[0] = quaternionA.x
            output[1] = quaternionA.y
            output[2] = quaternionA.z
            output[3] = quaternionA.w
            return
        }
        for (component in 0 until components) {
            val start = channel.values[lowBase + component]
            output[component] = start + (channel.values[highBase + component] - start) * factor
        }
    }

    private fun interpolateCubic(
        channel: AnimationChannel,
        low: Int,
        high: Int,
        factor: Float,
        duration: Float,
        output: FloatArray
    ) {
        val components = channel.componentCount
        val lowValue = (low * 3 + 1) * components
        val lowTangent = (low * 3 + 2) * components
        val highTangent = high * 3 * components
        val highValue = (high * 3 + 1) * components
        val factor2 = factor * factor
        val factor3 = factor2 * factor
        val h00 = 2.0f * factor3 - 3.0f * factor2 + 1.0f
        val h10 = factor3 - 2.0f * factor2 + factor
        val h01 = -2.0f * factor3 + 3.0f * factor2
        val h11 = factor3 - factor2
        for (component in 0 until components) {
            output[component] =
                h00 * channel.values[lowValue + component] +
                    h10 * duration * channel.values[lowTangent + component] +
                    h01 * channel.values[highValue + component] +
                    h11 * duration * channel.values[highTangent + component]
        }
        if (channel.path == AnimationPath.ROTATION && components == 4) {
            quaternionA.set(output[0], output[1], output[2], output[3]).normalize()
            output[0] = quaternionA.x
            output[1] = quaternionA.y
            output[2] = quaternionA.z
            output[3] = quaternionA.w
        }
    }

    private fun buildPose(factor: Float) {
        for (nodeIndex in asset.topologicalOrder) {
            val translationBase = nodeIndex * 3
            val rotationBase = nodeIndex * 4
            val node = asset.nodes[nodeIndex]
            val local = pose.localMatrices[nodeIndex]
            if (node.matrix != null && !animatedNodes[nodeIndex] && (fadeClipIndex < 0 || !secondaryAnimatedNodes[nodeIndex])) {
                local.set(node.matrix)
            } else {
                val tx = blend(primaryTranslation[translationBase], secondaryTranslation[translationBase], factor)
                val ty = blend(primaryTranslation[translationBase + 1], secondaryTranslation[translationBase + 1], factor)
                val tz = blend(primaryTranslation[translationBase + 2], secondaryTranslation[translationBase + 2], factor)
                quaternionA.set(
                    primaryRotation[rotationBase],
                    primaryRotation[rotationBase + 1],
                    primaryRotation[rotationBase + 2],
                    primaryRotation[rotationBase + 3]
                ).normalize()
                if (fadeClipIndex >= 0) {
                    quaternionB.set(
                        secondaryRotation[rotationBase],
                        secondaryRotation[rotationBase + 1],
                        secondaryRotation[rotationBase + 2],
                        secondaryRotation[rotationBase + 3]
                    ).normalize()
                    quaternionA.slerp(quaternionB, factor)
                }
                val sx = blend(primaryScale[translationBase], secondaryScale[translationBase], factor)
                val sy = blend(primaryScale[translationBase + 1], secondaryScale[translationBase + 1], factor)
                val sz = blend(primaryScale[translationBase + 2], secondaryScale[translationBase + 2], factor)
                local.identity().translationRotateScale(
                    tx,
                    ty,
                    tz,
                    quaternionA.x,
                    quaternionA.y,
                    quaternionA.z,
                    quaternionA.w,
                    sx,
                    sy,
                    sz
                )
            }
            val global = pose.globalMatrices[nodeIndex]
            if (node.parentIndex < 0) global.set(local) else global.set(pose.globalMatrices[node.parentIndex]).mul(local)
        }
        if (fadeClipIndex >= 0) {
            for (index in pose.morphWeights.indices) {
                pose.morphWeights[index] = blend(primaryMorph[index], secondaryMorph[index], factor)
            }
        } else {
            primaryMorph.copyInto(pose.morphWeights)
        }
        pose.materialUv.blendFrom(primaryMaterialUv, secondaryMaterialUv, factor)
        pose.materialFactor.blendFrom(primaryMaterialFactor, secondaryMaterialFactor, factor)
    }

    private fun normalizeTime(index: Int, time: Float): Float {
        if (index !in asset.animations.indices) return 0.0f
        val duration = asset.animations[index].durationSeconds
        if (duration <= 0.0f) return 0.0f
        if (!looping) return time.coerceIn(0.0f, duration)
        val value = time % duration
        return if (value < 0.0f) value + duration else value
    }

    private fun wrapped(previous: Float, current: Float): Boolean =
        looping && (speed >= 0.0f && current < previous || speed < 0.0f && current > previous)

    private fun resetKeys() {
        primaryKeys.fill(-1)
        secondaryKeys.fill(-1)
    }

    private fun maxComponentCount(): Int {
        var maximum = 4
        for (clip in asset.animations) {
            for (channel in clip.channels) {
                if (channel.componentCount > maximum) maximum = channel.componentCount
            }
        }
        return maximum
    }

    private fun maxChannelCount(): Int {
        var maximum = 0
        for (clip in asset.animations) {
            if (clip.channels.size > maximum) maximum = clip.channels.size
        }
        return maximum
    }

    private fun blend(start: Float, end: Float, factor: Float): Float =
        if (fadeClipIndex < 0) start else start + (end - start) * factor
}
