package com.micheanl.libgltf.model

import com.micheanl.libgltf.animation.AnimationClip
import com.micheanl.libgltf.material.GltfMaterial

data class GltfAsset(
    val name: String,
    val nodes: Array<GltfNode>,
    val topologicalOrder: IntArray,
    val sceneRoots: IntArray,
    val sceneNames: Array<String>,
    val sceneNodeMasks: Array<BooleanArray>,
    val defaultScene: Int,
    val meshes: Array<GltfMesh>,
    val skins: Array<GltfSkin>,
    val animations: Array<AnimationClip>,
    val materials: Array<GltfMaterial>,
    val materialVariantNames: Array<String>,
    val cameras: Array<GltfCamera>,
    val lights: Array<GltfLight>,
    val textures: Array<GltfTexture>,
    val images: Array<GltfImage>,
    val bounds: FloatArray,
    val stats: GltfStats,
    val morphOffsets: IntArray,
    val totalMorphWeights: Int
)
