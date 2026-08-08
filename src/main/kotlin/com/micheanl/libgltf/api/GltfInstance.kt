package com.micheanl.libgltf.api

import com.micheanl.libgltf.animation.AnimationController
import com.micheanl.libgltf.animation.AnimationPlayer
import com.micheanl.libgltf.lod.LodPolicy
import com.micheanl.libgltf.lod.LodSelector
import com.micheanl.libgltf.material.GltfMaterial
import com.micheanl.libgltf.material.MaterialOverride
import com.micheanl.libgltf.render.cpu.GltfGeometryRenderer
import com.micheanl.libgltf.render.feature.GltfGpuSubmit
import com.micheanl.libgltf.render.gpu.GpuAnimationState
import org.joml.Matrix4f
import org.joml.Matrix4fc

class GltfInstance internal constructor(val handle: GltfHandle) {
    val transform: Matrix4f = Matrix4f()
    val animation: AnimationPlayer = AnimationPlayer(handle.asset)
    val animator: AnimationController = AnimationController(handle.asset, animation)
    val animationState: GpuAnimationState = GpuAnimationState(handle.asset)
    val materialOverrides: Array<MaterialOverride?> = arrayOfNulls(handle.asset.materials.size)
    private val materialMappings: IntArray = IntArray(handle.asset.materials.size) { it }
    internal var sceneMask: BooleanArray = handle.asset.sceneNodeMasks[handle.asset.defaultScene]
    internal val geometryRenderers: Array<Array<GltfGeometryRenderer>> = createRenderers()
    internal val gpuSubmits: Array<Array<Array<GltfGpuSubmit>>> = createGpuSubmits()

    var renderMode: GltfRenderMode = GltfRenderMode.AUTO
    var automaticAnimation: Boolean = true
    var showBones: Boolean = false
    var materialVariant: Int = -1
    var sceneIndex: Int = handle.asset.defaultScene
        private set

    @Volatile
    var visible: Boolean = true

    var lodPolicy: LodPolicy = LodPolicy.DEFAULT
        set(value) {
            field = value
            lodSelector = LodSelector(value)
            lodLevel = 0
        }

    internal var lodSelector: LodSelector = LodSelector(lodPolicy)
    internal var lodLevel: Int = 0
    internal var lastUpdateNanos: Long = System.nanoTime()
    internal var animationRevision: Long = animation.revision
    internal var materialRevision: Long = 0L

    init {
        animationState.update(animation.pose)
        animationRevision = animation.revision
    }

    fun updateAnimation(deltaSeconds: Float): GltfInstance {
        animator.evaluate(deltaSeconds)
        syncAnimation()
        lastUpdateNanos = System.nanoTime()
        return this
    }

    fun syncAnimation(): GltfInstance {
        if (animationRevision != animation.revision) {
            animationState.update(animation.pose)
            animationRevision = animation.revision
        }
        return this
    }

    internal fun updateAnimationAt(nowNanos: Long) {
        if (automaticAnimation && nowNanos - lastUpdateNanos >= MIN_ANIMATION_INTERVAL_NANOS) {
            animator.evaluate((nowNanos - lastUpdateNanos) * 1.0e-9f)
            lastUpdateNanos = nowNanos
        }
        syncAnimation()
    }

    fun setTransform(value: Matrix4fc): GltfInstance {
        transform.set(value)
        return this
    }

    fun setPosition(x: Float, y: Float, z: Float): GltfInstance {
        transform.setTranslation(x, y, z)
        return this
    }

    fun material(index: Int): GltfMaterial {
        require(index in materialMappings.indices)
        return handle.asset.materials[materialMappings[index]]
    }

    fun material(name: String): GltfMaterial {
        val index = handle.asset.materials.indexOfFirst { it.name == name }
        require(index >= 0) { "Unknown material: $name" }
        return material(index)
    }

    fun remapMaterial(index: Int, replacement: Int): GltfInstance {
        require(index in materialMappings.indices)
        require(replacement in handle.asset.materials.indices)
        if (materialMappings[index] != replacement) {
            materialMappings[index] = replacement
            materialRevision++
        }
        return this
    }

    fun remapMaterial(name: String, replacement: String): GltfInstance {
        val index = handle.asset.materials.indexOfFirst { it.name == name }
        val replacementIndex = handle.asset.materials.indexOfFirst { it.name == replacement }
        require(index >= 0) { "Unknown material: $name" }
        require(replacementIndex >= 0) { "Unknown material: $replacement" }
        return remapMaterial(index, replacementIndex)
    }

    fun resetMaterials(): GltfInstance {
        for (index in materialMappings.indices) materialMappings[index] = index
        materialOverrides.fill(null)
        materialRevision++
        return this
    }

    fun selectVariant(index: Int): GltfInstance {
        val clamped = index.coerceIn(-1, handle.asset.materialVariantNames.lastIndex)
        if (materialVariant != clamped) {
            materialVariant = clamped
            materialRevision++
        }
        return this
    }

    fun selectScene(index: Int): GltfInstance {
        val clamped = index.coerceIn(0, handle.asset.sceneNodeMasks.lastIndex)
        if (sceneIndex != clamped) {
            sceneIndex = clamped
            sceneMask = handle.asset.sceneNodeMasks[clamped]
        }
        return this
    }

    fun setMaterial(index: Int, override: MaterialOverride?): GltfInstance {
        require(index in materialOverrides.indices)
        materialOverrides[index] = override
        materialRevision++
        return this
    }

    internal fun resolveMaterial(index: Int): Int = materialMappings[index.coerceIn(0, materialMappings.lastIndex)]

    internal fun resolvePrimitiveMaterial(sourceIndex: Int, variantMappings: IntArray): Int {
        val variant = materialVariant
        if (variant in variantMappings.indices) {
            val mapped = variantMappings[variant]
            if (mapped >= 0) return mapped
        }
        return resolveMaterial(sourceIndex)
    }

    private fun createRenderers(): Array<Array<GltfGeometryRenderer>> = Array(handle.asset.nodes.size) { nodeIndex ->
        val meshIndex = handle.asset.nodes[nodeIndex].meshIndex
        if (meshIndex < 0) emptyArray() else Array(handle.asset.meshes[meshIndex].primitives.size) { primitiveIndex ->
            GltfGeometryRenderer(this, nodeIndex, handle.asset.meshes[meshIndex].primitives[primitiveIndex])
        }
    }

    private fun createGpuSubmits(): Array<Array<Array<GltfGpuSubmit>>> =
        Array(handle.asset.nodes.size) { nodeIndex ->
        val meshIndex = handle.asset.nodes[nodeIndex].meshIndex
        if (meshIndex < 0) {
            emptyArray()
        } else {
            val instanceCount = (handle.asset.nodes[nodeIndex].instanceMatrices.size / 16).coerceAtLeast(1)
            Array(handle.asset.meshes[meshIndex].primitives.size) { primitiveIndex ->
                Array(instanceCount) { GltfGpuSubmit(this, nodeIndex, meshIndex, primitiveIndex) }
            }
        }
    }

    private companion object {
        const val MIN_ANIMATION_INTERVAL_NANOS = 1_000_000L
    }
}
