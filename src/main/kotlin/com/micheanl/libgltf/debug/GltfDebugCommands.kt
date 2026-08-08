package com.micheanl.libgltf.debug

import com.micheanl.libgltf.api.GltfApiImpl
import com.micheanl.libgltf.api.GltfHandle
import com.micheanl.libgltf.api.GltfInstance
import com.micheanl.libgltf.api.GltfInstanceId
import com.micheanl.libgltf.api.GltfRenderMode
import com.micheanl.libgltf.asset.GltfLoadFailure
import com.micheanl.libgltf.asset.GltfLoadSuccess
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.nio.file.Files
import java.nio.file.Path

object GltfDebugCommands {
    private var handle: GltfHandle? = null
    private var instance: GltfInstance? = null
    private var instanceId: GltfInstanceId? = null

    fun initialize() {
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
                    .then(ClientCommands.literal("mode").then(ClientCommands.literal("auto").executes { mode(it.source, GltfRenderMode.AUTO) }))
                    .then(ClientCommands.literal("mode").then(ClientCommands.literal("gpu").executes { mode(it.source, GltfRenderMode.GPU_PREFERRED) }))
                    .then(ClientCommands.literal("mode").then(ClientCommands.literal("cpu").executes { mode(it.source, GltfRenderMode.CPU) }))
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
                        val newInstance = GltfApiImpl.createInstance(newHandle)
                            .setPosition(player.x.toFloat(), player.y.toFloat() + 1.0f, player.z.toFloat())
                        instanceId = GltfApiImpl.register(newInstance)
                        handle = newHandle
                        instance = newInstance
                        source.sendFeedback(
                            Component.literal(
                                "Loaded ${result.asset.name}: " +
                                    "${result.asset.stats.nodeCount} nodes, " +
                                    "${result.asset.stats.triangleCount} triangles"
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
        source?.sendFeedback(Component.literal("Unloaded external model"))
        return 0
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
}
