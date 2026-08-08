package com.micheanl.libgltf.debug

import com.micheanl.libgltf.api.GltfApiImpl
import com.micheanl.libgltf.api.GltfHandle
import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.api.GltfInstanceId
import com.micheanl.libgltf.api.GltfRenderMode
import com.micheanl.libgltf.asset.GltfLoadFailure
import com.micheanl.libgltf.asset.GltfLoadSuccess
import com.micheanl.libgltf.LibGltf
import com.micheanl.libgltf.render.GltfRenderRegistry
import com.micheanl.libgltf.render.GltfSceneRenderer
import com.micheanl.libgltf.render.feature.GltfGpuFeatureRenderer
import com.micheanl.libgltf.render.gpu.GltfGpuBackend
import com.micheanl.libgltf.render.vulkan.GltfGpuDrivenSettings
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.debug.DebugScreenEntries
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus
import net.minecraft.network.chat.Component
import java.nio.file.Files
import java.nio.file.Path

object GltfDebugCommands {
    private var handle: GltfHandle? = null
    private var instance: GltfInstance? = null
    private var instanceId: GltfInstanceId? = null
    private var loadedName: String = ""
    private var loadedStats: String = ""

    fun initialize() {
        val entryId = DebugScreenEntries.register(LibGltf.id("debug"), GltfDebugEntry())
        ClientLifecycleEvents.CLIENT_STARTED.register {
            Minecraft.getInstance().debugEntries.setStatus(entryId, DebugScreenEntryStatus.IN_OVERLAY)
        }
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("libgltf_debug")
                    .then(
                        ClientCommands.literal("load")
                            .then(
                                ClientCommands.argument("path", StringArgumentType.greedyString())
                                    .executes { context ->
                                        load(
                                            context.source,
                                            context.getArgument("path", String::class.java)
                                        )
                                    }
                            )
                    )
                    .then(ClientCommands.literal("unload").executes { unload(it.source) })
                    .then(
                        ClientCommands.literal("pos")
                            .then(
                                ClientCommands.argument("x", FloatArgumentType.floatArg())
                                    .then(
                                        ClientCommands.argument("y", FloatArgumentType.floatArg())
                                            .then(
                                                ClientCommands.argument("z", FloatArgumentType.floatArg())
                                                    .executes { context ->
                                                        position(
                                                            context.source,
                                                            context.getArgument("x", Float::class.java),
                                                            context.getArgument("y", Float::class.java),
                                                            context.getArgument("z", Float::class.java)
                                                        )
                                                    }
                                            )
                                    )
                            )
                    )
                    .then(
                        ClientCommands.literal("scale")
                            .then(
                                ClientCommands.argument("scale", FloatArgumentType.floatArg(0.001f))
                                    .executes { context ->
                                        scale(context.source, context.getArgument("scale", Float::class.java))
                                    }
                            )
                    )
                    .then(
                        ClientCommands.literal("lod")
                            .then(
                                ClientCommands.argument("level", IntegerArgumentType.integer(0))
                                    .executes { context ->
                                        lod(context.source, context.getArgument("level", Int::class.java))
                                    }
                            )
                    )
                    .then(ClientCommands.literal("info").executes { info(it.source) })
                    .then(
                        ClientCommands.literal("benchmark")
                            .then(ClientCommands.literal("on").executes { benchmark(it.source, true) })
                            .then(ClientCommands.literal("off").executes { benchmark(it.source, false) })
                    )
                    .then(
                        ClientCommands.literal("anim")
                            .executes { playAnim(it.source, 0) }
                            .then(
                                ClientCommands.argument("index", IntegerArgumentType.integer(0))
                                    .executes { context ->
                                        playAnim(context.source, context.getArgument("index", Int::class.java))
                                    }
                            )
                            .then(ClientCommands.literal("stop").executes { stopAnim(it.source) })
                    )
                    .then(ClientCommands.literal("mode").then(ClientCommands.literal("auto").executes { mode(it.source, GltfRenderMode.AUTO) }))
                    .then(ClientCommands.literal("mode").then(ClientCommands.literal("gpu").executes { mode(it.source, GltfRenderMode.GPU_PREFERRED) }))
                    .then(ClientCommands.literal("mode").then(ClientCommands.literal("cpu").executes { mode(it.source, GltfRenderMode.CPU) }))
                    .then(
                        ClientCommands.literal("bones")
                            .then(ClientCommands.literal("on").executes { bones(it.source, true) })
                            .then(ClientCommands.literal("off").executes { bones(it.source, false) })
                    )
                    .then(
                        ClientCommands.literal("variant")
                            .then(
                                ClientCommands.argument("index", IntegerArgumentType.integer(-1))
                                    .executes { context ->
                                        variant(
                                            context.source,
                                            context.getArgument("index", Int::class.java)
                                        )
                                    }
                            )
                    )
                    .then(
                        ClientCommands.literal("scene")
                            .then(
                                ClientCommands.argument("index", IntegerArgumentType.integer(0))
                                    .executes { context ->
                                        scene(
                                            context.source,
                                            context.getArgument("index", Int::class.java)
                                        )
                                    }
                            )
                    )
                    .then(
                        ClientCommands.literal("mesh")
                            .then(ClientCommands.literal("on").executes { mesh(it.source, true) })
                            .then(ClientCommands.literal("off").executes { mesh(it.source, false) })
                            .then(ClientCommands.literal("auto").executes { meshAuto(it.source) })
                            .then(
                                ClientCommands.literal("cull")
                                    .then(ClientCommands.literal("on").executes { cull(it.source, true) })
                                    .then(ClientCommands.literal("off").executes { cull(it.source, false) })
                            )
                            .then(
                                ClientCommands.literal("occlusion")
                                    .then(ClientCommands.literal("on").executes { meshOcclusion(it.source, true) })
                                    .then(ClientCommands.literal("off").executes { meshOcclusion(it.source, false) })
                            )
                            .then(
                                ClientCommands.literal("minimal")
                                    .then(ClientCommands.literal("on").executes { meshMinimal(it.source, true) })
                                    .then(ClientCommands.literal("off").executes { meshMinimal(it.source, false) })
                            )
                            .then(
                                ClientCommands.literal("flat")
                                    .then(ClientCommands.literal("on").executes { meshFlat(it.source, true) })
                                    .then(ClientCommands.literal("off").executes { meshFlat(it.source, false) })
                            )
                            .then(
                                ClientCommands.literal("counters")
                                    .then(ClientCommands.literal("on").executes { meshCounters(it.source, true) })
                                    .then(ClientCommands.literal("off").executes { meshCounters(it.source, false) })
                            )
                            .then(
                                ClientCommands.literal("limit")
                                    .then(
                                        ClientCommands.argument("groups", IntegerArgumentType.integer(0))
                                            .executes { context ->
                                                meshLimit(
                                                    context.source,
                                                    context.getArgument("groups", Int::class.java)
                                                )
                                            }
                                    )
                            )
                    )
            )
        }
    }

    private fun load(source: FabricClientCommandSource, path: String): Int {
        val cleaned = path.trim().removeSurrounding("\"")
        val file = Path.of(cleaned)
        if (!Files.isRegularFile(file)) {
            source.sendError(Component.literal("File not found: $cleaned"))
            return 1
        }
        unload(null)
        GltfApiImpl.loadAsync(file).whenComplete { result, error ->
            Minecraft.getInstance().execute {
                if (error != null) {
                    source.sendError(Component.literal("Load failed: ${error.message ?: error.javaClass.simpleName}"))
                    return@execute
                }
                when (result) {
                    is GltfLoadSuccess -> {
                        val player = source.player
                        val newHandle = GltfApiImpl.upload(result.asset)
                        val yaw = Math.toRadians(player.getYRot().toDouble())
                        val forwardX = (-Math.sin(yaw)).toFloat()
                        val forwardZ = Math.cos(yaw).toFloat()
                        val newInstance = GltfApiImpl.createInstance(newHandle)
                            .setPosition(
                                player.x.toFloat() + forwardX * SPAWN_DISTANCE,
                                player.y.toFloat() + 1.0f,
                                player.z.toFloat() + forwardZ * SPAWN_DISTANCE
                            )
                        if (result.asset.animations.isNotEmpty()) {
                            newInstance.animator.play(newInstance.animator.segment(0))
                        }
                        instanceId = GltfApiImpl.register(newInstance)
                        handle = newHandle
                        instance = newInstance
                        loadedName = result.asset.name
                        loadedStats = "${result.asset.stats.nodeCount} nodes, ${result.asset.stats.triangleCount} tris"
                        source.sendFeedback(
                            Component.literal(
                                "Loaded $loadedName: $loadedStats"
                            )
                        )
                    }
                    is GltfLoadFailure -> source.sendError(Component.literal("Load failed: ${result.message}"))
                }
            }
        }
        return 0
    }

    private fun unload(source: FabricClientCommandSource?): Int {
        instanceId?.let(GltfApiImpl::unregister)
        handle?.close()
        instanceId = null
        handle = null
        instance = null
        loadedName = ""
        loadedStats = ""
        source?.sendFeedback(Component.literal("Unloaded external model"))
        return 0
    }

    fun statusLines(): List<String> {
        val capabilities = GltfGpuBackend.capabilities()
        val profile = GltfGpuBackend.vendorProfile()
        val oit = Minecraft.getInstance().gameRenderer.useImprovedTransparency()
        val lines = ArrayList<String>()
        lines += "libgltf backend=${capabilities.backend} vendor=${profile.vendor} path=${capabilities.path}"
        val meshOverride = GltfGpuDrivenSettings.meshShaderOverride
        val meshMode = when (meshOverride) {
            true -> "on"
            false -> "off"
            null -> "auto"
        }
        lines += "libgltf mesh=${if (GltfGpuFeatureRenderer.activeMesh) "on" else "off"}($meshMode) " +
            "minimal=${if (GltfGpuDrivenSettings.debugMeshMinimal) "on" else "off"} " +
            "flat=${if (GltfGpuDrivenSettings.debugMeshFlat) "on" else "off"} " +
            "counters=${if (GltfGpuDrivenSettings.debugMeshCounters) "on" else "off"} " +
            "limit=${GltfGpuDrivenSettings.meshGroupLimit} " +
            "cull=${if (GltfGpuDrivenSettings.instanceCulling && GltfGpuDrivenSettings.meshletCulling) "on" else "off"} " +
            "occlusion=${if (GltfGpuDrivenSettings.occlusionCulling) "on" else "off"} " +
            "bench=${if (GltfGpuDrivenSettings.benchmark) "on" else "off"} " +
            "oit=${if (oit) "on" else "off"}"
        lines += "libgltf gpu=${GltfSceneRenderer.lastGpuSubmits} " +
            "cpu=${GltfSceneRenderer.lastCpuSubmits} " +
            "culled=${GltfSceneRenderer.lastCulledPrimitives} " +
            "batches=${GltfGpuFeatureRenderer.lastFrameBatches} " +
            "drawnInstances=${GltfGpuFeatureRenderer.lastFrameInstances}"
        if (loadedName.isEmpty()) {
            lines += "libgltf model=none"
        } else {
            lines += "libgltf model=$loadedName ($loadedStats)"
            val current = instance
            if (current != null) {
                val transform = current.transform
                lines += "libgltf mode=${current.renderMode} " +
                    "pos=(${transform.m30()}, ${transform.m31()}, ${transform.m32()}) " +
                    "scale=${transform.m00()} lod=${current.lodLevel} " +
                    "anim=${current.animation.clipIndex} " +
                    "uv=${if (current.animation.pose.materialUv.animated.any { it }) "on" else "off"} " +
                    "bones=${if (current.showBones) "on" else "off"} " +
                    "variant=${variantLabel(current)} " +
                    "scene=${sceneLabel(current)} " +
                    "instances=${GltfRenderRegistry.instances().size}"
            }
        }
        return lines
    }

    private fun position(source: FabricClientCommandSource, x: Float, y: Float, z: Float): Int {
        val current = instance
        if (current == null) {
            source.sendError(Component.literal("No model loaded"))
            return 1
        }
        current.setPosition(x, y, z)
        source.sendFeedback(Component.literal("Position set to ($x, $y, $z)"))
        return 0
    }

    private fun scale(source: FabricClientCommandSource, value: Float): Int {
        val current = instance
        if (current == null) {
            source.sendError(Component.literal("No model loaded"))
            return 1
        }
        current.transform.scale(value)
        source.sendFeedback(Component.literal("Scale set to $value"))
        return 0
    }

    private fun lod(source: FabricClientCommandSource, level: Int): Int {
        val current = instance
        if (current == null) {
            source.sendError(Component.literal("No model loaded"))
            return 1
        }
        current.lodLevel = level.coerceAtLeast(0)
        source.sendFeedback(Component.literal("LOD set to ${current.lodLevel}"))
        return 0
    }

    private fun info(source: FabricClientCommandSource): Int {
        val current = instance
        val currentHandle = handle
        if (current == null || currentHandle == null) {
            source.sendError(Component.literal("No model loaded"))
            return 1
        }
        val asset = currentHandle.asset
        source.sendFeedback(
            Component.literal(
                "nodes=${asset.nodes.size} meshes=${asset.meshes.size} lod=${current.lodLevel} " +
                    "variants=${asset.materialVariantNames.size} scenes=${asset.sceneNames.size} " +
                    "cameras=${asset.cameras.size} lights=${asset.lights.size}"
            )
        )
        for (nodeIndex in asset.topologicalOrder) {
            val node = asset.nodes[nodeIndex]
            if (node.meshIndex < 0) continue
            val global = current.animation.pose.globalMatrices[nodeIndex]
            val instanceCount = node.instanceMatrices.size / 16
            source.sendFeedback(
                Component.literal(
                    "node[$nodeIndex] ${node.name} mesh=${node.meshIndex} " +
                        "t=(${global.m30()}, ${global.m31()}, ${global.m32()})" +
                        if (instanceCount > 0) " inst=$instanceCount" else ""
                )
            )
        }
        return 0
    }

    private fun playAnim(source: FabricClientCommandSource, index: Int): Int {
        val current = instance
        if (current == null) {
            source.sendError(Component.literal("No model loaded"))
            return 1
        }
        val animations = current.handle.asset.animations
        if (animations.isEmpty()) {
            source.sendError(Component.literal("Model has no animations"))
            return 1
        }
        val clip = index.coerceIn(0, animations.lastIndex)
        current.animator.play(current.animator.segment(clip))
        source.sendFeedback(Component.literal("Playing animation $clip: ${animations[clip].name}"))
        return 0
    }

    private fun stopAnim(source: FabricClientCommandSource): Int {
        val current = instance
        if (current == null) {
            source.sendError(Component.literal("No model loaded"))
            return 1
        }
        current.animator.stop()
        source.sendFeedback(Component.literal("Animation stopped"))
        return 0
    }

    private fun mode(source: FabricClientCommandSource, value: GltfRenderMode): Int {
        val current = instance
        if (current == null) {
            source.sendError(Component.literal("No model loaded"))
            return 1
        }
        current.renderMode = value
        source.sendFeedback(Component.literal("Render mode set to $value"))
        return 0
    }

    private fun bones(source: FabricClientCommandSource, value: Boolean): Int {
        val current = instance
        if (current == null) {
            source.sendError(Component.literal("No model loaded"))
            return 1
        }
        current.showBones = value
        source.sendFeedback(Component.literal("Bone debug set to $value"))
        return 0
    }

    private fun variant(source: FabricClientCommandSource, index: Int): Int {
        val current = instance
        if (current == null) {
            source.sendError(Component.literal("No model loaded"))
            return 1
        }
        val names = current.handle.asset.materialVariantNames
        if (names.isEmpty()) {
            source.sendError(Component.literal("Model has no material variants"))
            return 1
        }
        if (index < -1 || index >= names.size) {
            source.sendError(Component.literal("Variant index must be in -1..${names.lastIndex}"))
            return 1
        }
        current.selectVariant(index)
        val label = if (index < 0) "default" else "${names[index]} ($index)"
        source.sendFeedback(Component.literal("Material variant set to $label"))
        return 0
    }

    private fun variantLabel(current: GltfInstance): String {
        val names = current.handle.asset.materialVariantNames
        val index = current.materialVariant
        return if (index in names.indices) "${names[index]} ($index)" else "default"
    }

    private fun scene(source: FabricClientCommandSource, index: Int): Int {
        val current = instance
        if (current == null) {
            source.sendError(Component.literal("No model loaded"))
            return 1
        }
        val names = current.handle.asset.sceneNames
        if (names.isEmpty()) {
            source.sendError(Component.literal("Model has no scenes"))
            return 1
        }
        if (index !in names.indices) {
            source.sendError(Component.literal("Scene index must be in 0..${names.lastIndex}"))
            return 1
        }
        current.selectScene(index)
        source.sendFeedback(Component.literal("Scene set to ${names[index]} ($index)"))
        return 0
    }

    private fun sceneLabel(current: GltfInstance): String {
        val names = current.handle.asset.sceneNames
        val index = current.sceneIndex
        return if (names.isEmpty()) "default" else "${names[index]} ($index)"
    }

    private fun mesh(source: FabricClientCommandSource, value: Boolean): Int {
        Minecraft.getInstance().execute { GltfGpuFeatureRenderer.setMeshShader(value) }
        source.sendFeedback(Component.literal("Mesh shader set to $value"))
        return 0
    }

    private fun meshAuto(source: FabricClientCommandSource): Int {
        Minecraft.getInstance().execute { GltfGpuFeatureRenderer.resetMeshShader() }
        source.sendFeedback(Component.literal("Mesh shader set to auto"))
        return 0
    }

    private fun cull(source: FabricClientCommandSource, value: Boolean): Int {
        GltfGpuDrivenSettings.instanceCulling = value
        GltfGpuDrivenSettings.meshletCulling = value
        source.sendFeedback(Component.literal("Mesh culling set to $value"))
        return 0
    }

    private fun meshOcclusion(source: FabricClientCommandSource, value: Boolean): Int {
        Minecraft.getInstance().execute {
            GltfGpuDrivenSettings.occlusionCulling = value
            GltfGpuFeatureRenderer.recreate()
        }
        source.sendFeedback(Component.literal("Mesh occlusion culling set to $value"))
        return 0
    }

    private fun meshMinimal(source: FabricClientCommandSource, value: Boolean): Int {
        Minecraft.getInstance().execute {
            GltfGpuDrivenSettings.debugMeshMinimal = value
            GltfGpuFeatureRenderer.recreate()
        }
        source.sendFeedback(Component.literal("Mesh minimal debug set to $value"))
        return 0
    }

    private fun meshFlat(source: FabricClientCommandSource, value: Boolean): Int {
        Minecraft.getInstance().execute {
            GltfGpuDrivenSettings.debugMeshFlat = value
            GltfGpuFeatureRenderer.recreate()
        }
        source.sendFeedback(Component.literal("Mesh flat debug set to $value"))
        return 0
    }

    private fun meshCounters(source: FabricClientCommandSource, value: Boolean): Int {
        Minecraft.getInstance().execute {
            GltfGpuDrivenSettings.debugMeshCounters = value
            GltfGpuFeatureRenderer.recreate()
        }
        source.sendFeedback(Component.literal("Mesh debug counters set to $value"))
        return 0
    }

    private fun meshLimit(source: FabricClientCommandSource, groups: Int): Int {
        GltfGpuDrivenSettings.meshGroupLimit = groups
        source.sendFeedback(Component.literal("Mesh group limit set to $groups"))
        return 0
    }

    private fun benchmark(source: FabricClientCommandSource, value: Boolean): Int {
        GltfGpuDrivenSettings.benchmark = value
        source.sendFeedback(Component.literal("Benchmark set to $value"))
        return 0
    }

    private const val SPAWN_DISTANCE = 5.0f
}
