package com.micheanl.libgltf.debug

import com.micheanl.libgltf.LibGltf
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer
import net.minecraft.client.gui.components.debug.DebugScreenEntry
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk

class GltfDebugEntry : DebugScreenEntry {
    override fun display(
        displayer: DebugScreenDisplayer,
        serverOrClientLevel: Level?,
        clientChunk: LevelChunk?,
        serverChunk: LevelChunk?
    ) {
        displayer.addToGroup(GROUP, GltfDebugCommands.statusLines())
    }

    companion object {
        val GROUP: Identifier = LibGltf.id("debug")
    }
}
