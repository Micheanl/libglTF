package com.micheanl.libgltf.asset

import com.micheanl.libgltf.util.JsonValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromStream
import com.micheanl.libgltf.animation.AnimationChannel
import com.micheanl.libgltf.animation.AnimationClip
import com.micheanl.libgltf.animation.AnimationPath
import com.micheanl.libgltf.animation.Interpolation
import com.micheanl.libgltf.lod.LodPolicy
import com.micheanl.libgltf.lod.MeshLodBuilder
import com.micheanl.libgltf.material.AlphaMode
import com.micheanl.libgltf.material.AnisotropyMaterial
import com.micheanl.libgltf.material.ClearcoatMaterial
import com.micheanl.libgltf.material.GltfMaterial
import com.micheanl.libgltf.material.IridescenceMaterial
import com.micheanl.libgltf.material.SheenMaterial
import com.micheanl.libgltf.material.SpecularMaterial
import com.micheanl.libgltf.material.TextureBinding
import com.micheanl.libgltf.material.TextureFilter
import com.micheanl.libgltf.material.TextureSampler
import com.micheanl.libgltf.material.TextureWrap
import com.micheanl.libgltf.model.CameraType
import com.micheanl.libgltf.model.GltfAsset
import com.micheanl.libgltf.model.GltfCamera
import com.micheanl.libgltf.model.GltfImage
import com.micheanl.libgltf.model.GltfLight
import com.micheanl.libgltf.model.GltfMesh
import com.micheanl.libgltf.model.GltfNode
import com.micheanl.libgltf.model.LightType
import com.micheanl.libgltf.model.GltfPrimitive
import com.micheanl.libgltf.model.GltfSkin
import com.micheanl.libgltf.model.GltfStats
import com.micheanl.libgltf.model.GltfTexture
import com.micheanl.libgltf.model.PrimitiveMode
import com.micheanl.libgltf.model.VertexLayout
import com.micheanl.libgltf.util.JsonFields
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.joml.Matrix4f

object GltfLoader {
    private val json = Json {
        isLenient = false
        allowSpecialFloatingPointValues = false
    }

    private val executor: ExecutorService = Executors.newFixedThreadPool(
        (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1),
        Thread.ofPlatform().daemon().name("libgltf-loader-", 0).factory()
    )

    fun loadAsync(path: Path, lodPolicy: LodPolicy = LodPolicy.DEFAULT): CompletableFuture<GltfLoadResult> =
        CompletableFuture.supplyAsync({ load(path, lodPolicy) }, executor)

    fun load(path: Path, lodPolicy: LodPolicy = LodPolicy.DEFAULT): GltfLoadResult = try {
        GltfLoadSuccess(parse(path, lodPolicy))
    } catch (throwable: Throwable) {
        GltfLoadFailure(throwable.message ?: throwable.javaClass.simpleName, throwable)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun parse(path: Path, lodPolicy: LodPolicy): GltfAsset {
        val bytes = Files.readAllBytes(path)
        val source = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val payload = if (GlbReader.isGlb(source)) {
            GlbReader.read(source)
        } else {
            GlbPayload(bytes, null)
        }
        val root = JsonValue(
            json.decodeFromStream(
                JsonElement.serializer(),
                ByteArrayInputStream(payload.json)
            )
        )
        val asset = JsonFields.value(root, "asset") ?: error("asset is missing")
        require(JsonFields.string(asset, "version") == "2.0")
        val basePath = path.parent ?: Path.of(".")
        val resolver = GltfBufferResolver(root, basePath, payload.binary)
        val decoder = AccessorDecoder(root, resolver)
        val images = parseImages(root, resolver)
        val textures = parseTextures(root)
        val materials = parseMaterials(root)
        val materialVariantNames = parseMaterialVariantNames(root)
        val meshes = parseMeshes(root, decoder, lodPolicy, materialVariantNames.size)
        val nodes = parseNodes(root, decoder)
        val parents = parentIndices(nodes)
        val resolvedNodes = Array(nodes.size) { index -> nodes[index].copy(parentIndex = parents[index]) }
        val roots = sceneRoots(root, parents)
        val order = topologicalOrder(resolvedNodes, roots)
        val scenes = JsonFields.value(root, "scenes")
        val sceneNames: Array<String>
        val sceneNodeMasks: Array<BooleanArray>
        val defaultScene: Int
        if (scenes == null || scenes.size() == 0) {
            sceneNames = emptyArray()
            sceneNodeMasks = arrayOf(BooleanArray(resolvedNodes.size) { true })
            defaultScene = 0
        } else {
            defaultScene = JsonFields.int(root, "scene", 0).coerceIn(0, scenes.size() - 1)
            sceneNames = Array(scenes.size()) { sceneIndex ->
                JsonFields.string(scenes[sceneIndex], "name", "scene_$sceneIndex")
            }
            sceneNodeMasks = Array(scenes.size()) { sceneIndex ->
                val mask = BooleanArray(resolvedNodes.size)
                for (root in JsonFields.ints(scenes[sceneIndex], "nodes")) {
                    markSceneMask(root, mask, resolvedNodes)
                }
                mask
            }
        }
        val skins = parseSkins(root, decoder)
        val cameras = parseCameras(root)
        val lights = parseLights(root)
        val animations = parseAnimations(root, decoder)
        val morphOffsets = IntArray(resolvedNodes.size)
        var totalMorphWeights = 0
        for (index in resolvedNodes.indices) {
            morphOffsets[index] = totalMorphWeights
            val meshIndex = resolvedNodes[index].meshIndex
            if (meshIndex >= 0) {
                val mesh = meshes[meshIndex]
                val count = if (mesh.weights.isNotEmpty()) mesh.weights.size else mesh.primitives.maxOfOrNull { it.morphTargetCount } ?: 0
                totalMorphWeights += count
            }
        }
        val bounds = assetBounds(meshes)
        val primitiveCount = meshes.sumOf { it.primitives.size }
        val triangleCount = meshes.sumOf { mesh ->
            mesh.primitives.sumOf { primitive ->
                if (primitive.mode == PrimitiveMode.TRIANGLES) primitive.lodIndices[0].size.toLong() / 3L else 0L
            }
        }
        val morphTargetCount = meshes.sumOf { mesh -> mesh.primitives.sumOf { it.morphTargetCount } }
        val stats = GltfStats(
            resolvedNodes.size,
            meshes.size,
            primitiveCount,
            triangleCount,
            materials.size,
            textures.size,
            skins.size,
            morphTargetCount
        )
        return GltfAsset(
            path.fileName.toString(),
            resolvedNodes,
            order,
            roots,
            sceneNames,
            sceneNodeMasks,
            defaultScene,
            meshes,
            skins,
            animations,
            materials,
            materialVariantNames,
            cameras,
            lights,
            textures,
            images,
            bounds,
            stats,
            morphOffsets,
            totalMorphWeights
        )
    }

    private fun parseImages(root: JsonValue, resolver: GltfBufferResolver): Array<GltfImage> {
        val values = JsonFields.value(root, "images") ?: return emptyArray()
        return Array(values.size()) { index ->
            val image = values[index]
            val uri = JsonFields.string(image, "uri")
            val bytes = if (uri.isNotEmpty()) {
                resolver.readUri(uri)
            } else {
                val view = resolver.view(JsonFields.int(image, "bufferView"))
                ByteArray(view.remaining()).also(view::get)
            }
            val mime = JsonFields.string(image, "mimeType", mimeFromUri(uri))
            GltfImage(JsonFields.string(image, "name", "image_$index"), mime, bytes)
        }
    }

    private fun parseTextures(root: JsonValue): Array<GltfTexture> {
        val values = JsonFields.value(root, "textures") ?: return emptyArray()
        val samplers = JsonFields.value(root, "samplers")
        return Array(values.size()) { index ->
            val texture = values[index]
            val samplerIndex = JsonFields.int(texture, "sampler")
            val sampler = if (samplerIndex >= 0 && samplers != null) parseSampler(samplers[samplerIndex]) else TextureSampler.DEFAULT
            GltfTexture(JsonFields.int(texture, "source"), sampler)
        }
    }

    private fun parseSampler(value: JsonValue): TextureSampler {
        val mag = when (JsonFields.int(value, "magFilter", 9729)) {
            9728 -> TextureFilter.NEAREST
            else -> TextureFilter.LINEAR
        }
        val minCode = JsonFields.int(value, "minFilter", 9987)
        val min = when (minCode) {
            9728, 9984, 9986 -> TextureFilter.NEAREST
            else -> TextureFilter.LINEAR
        }
        val mipmap = when (minCode) {
            9984, 9985 -> TextureFilter.NEAREST
            9986, 9987 -> TextureFilter.LINEAR
            else -> null
        }
        return TextureSampler(
            mag,
            min,
            mipmap,
            wrap(JsonFields.int(value, "wrapS", 10497)),
            wrap(JsonFields.int(value, "wrapT", 10497))
        )
    }

    private fun parseMaterials(root: JsonValue): Array<GltfMaterial> {
        val values = JsonFields.value(root, "materials") ?: return arrayOf(defaultMaterial())
        return Array(values.size()) { index ->
            val value = values[index]
            val pbr = JsonFields.value(value, "pbrMetallicRoughness")
            val extensions = JsonFields.value(value, "extensions")
            val emissiveExtension = JsonFields.value(extensions, "KHR_materials_emissive_strength")
            val specularExtension = JsonFields.value(extensions, "KHR_materials_specular")
            val clearcoatExtension = JsonFields.value(extensions, "KHR_materials_clearcoat")
            val sheenExtension = JsonFields.value(extensions, "KHR_materials_sheen")
            val transmissionExtension = JsonFields.value(extensions, "KHR_materials_transmission")
            val volumeExtension = JsonFields.value(extensions, "KHR_materials_volume")
            val iorExtension = JsonFields.value(extensions, "KHR_materials_ior")
            val dispersionExtension = JsonFields.value(extensions, "KHR_materials_dispersion")
            val anisotropyExtension = JsonFields.value(extensions, "KHR_materials_anisotropy")
            val iridescenceExtension = JsonFields.value(extensions, "KHR_materials_iridescence")
            GltfMaterial(
                JsonFields.string(value, "name", "material_$index"),
                JsonFields.floats(pbr, "baseColorFactor", floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)),
                parseBinding(JsonFields.value(pbr, "baseColorTexture")),
                JsonFields.float(pbr, "metallicFactor", 1.0f),
                JsonFields.float(pbr, "roughnessFactor", 1.0f),
                parseBinding(JsonFields.value(pbr, "metallicRoughnessTexture")),
                parseBinding(JsonFields.value(value, "normalTexture")),
                JsonFields.float(JsonFields.value(value, "normalTexture"), "scale", 1.0f),
                parseBinding(JsonFields.value(value, "occlusionTexture")),
                JsonFields.float(JsonFields.value(value, "occlusionTexture"), "strength", 1.0f),
                parseBinding(JsonFields.value(value, "emissiveTexture")),
                JsonFields.floats(value, "emissiveFactor", floatArrayOf(0.0f, 0.0f, 0.0f)),
                JsonFields.float(emissiveExtension, "emissiveStrength", 1.0f),
                alphaMode(JsonFields.string(value, "alphaMode", "OPAQUE")),
                JsonFields.float(value, "alphaCutoff", 0.5f),
                JsonFields.boolean(value, "doubleSided"),
                JsonFields.value(extensions, "KHR_materials_unlit") != null,
                specularExtension?.let {
                    SpecularMaterial(
                        JsonFields.float(it, "specularFactor", 1.0f),
                        parseBinding(JsonFields.value(it, "specularTexture")),
                        JsonFields.floats(it, "specularColorFactor", floatArrayOf(1.0f, 1.0f, 1.0f)),
                        parseBinding(JsonFields.value(it, "specularColorTexture"))
                    )
                },
                clearcoatExtension?.let {
                    ClearcoatMaterial(
                        JsonFields.float(it, "clearcoatFactor"),
                        parseBinding(JsonFields.value(it, "clearcoatTexture")),
                        JsonFields.float(it, "clearcoatRoughnessFactor"),
                        parseBinding(JsonFields.value(it, "clearcoatRoughnessTexture")),
                        parseBinding(JsonFields.value(it, "clearcoatNormalTexture")),
                        JsonFields.float(JsonFields.value(it, "clearcoatNormalTexture"), "scale", 1.0f)
                    )
                },
                sheenExtension?.let {
                    SheenMaterial(
                        JsonFields.floats(it, "sheenColorFactor", floatArrayOf(0.0f, 0.0f, 0.0f)),
                        parseBinding(JsonFields.value(it, "sheenColorTexture")),
                        JsonFields.float(it, "sheenRoughnessFactor"),
                        parseBinding(JsonFields.value(it, "sheenRoughnessTexture"))
                    )
                },
                JsonFields.float(transmissionExtension, "transmissionFactor"),
                parseBinding(JsonFields.value(transmissionExtension, "transmissionTexture")),
                JsonFields.float(volumeExtension, "thicknessFactor"),
                parseBinding(JsonFields.value(volumeExtension, "thicknessTexture")),
                JsonFields.float(volumeExtension, "attenuationDistance", Float.POSITIVE_INFINITY),
                JsonFields.floats(volumeExtension, "attenuationColor", floatArrayOf(1.0f, 1.0f, 1.0f)),
                JsonFields.float(iorExtension, "ior", 1.5f),
                JsonFields.float(dispersionExtension, "dispersion"),
                anisotropyExtension?.let {
                    AnisotropyMaterial(
                        JsonFields.float(it, "anisotropyStrength"),
                        JsonFields.float(it, "anisotropyRotation"),
                        parseBinding(JsonFields.value(it, "anisotropyTexture"))
                    )
                },
                iridescenceExtension?.let {
                    IridescenceMaterial(
                        JsonFields.float(it, "iridescenceFactor"),
                        parseBinding(JsonFields.value(it, "iridescenceTexture")),
                        JsonFields.float(it, "iridescenceIor", 1.3f),
                        JsonFields.float(it, "iridescenceThicknessMinimum", 100.0f),
                        JsonFields.float(it, "iridescenceThicknessMaximum", 400.0f),
                        parseBinding(JsonFields.value(it, "iridescenceThicknessTexture"))
                    )
                }
            )
        }
    }
    private fun parseBinding(value: JsonValue?): TextureBinding? {
        if (value == null) return null
        val transform = JsonFields.value(JsonFields.value(value, "extensions"), "KHR_texture_transform")
        val offset = JsonFields.floats(transform, "offset", floatArrayOf(0.0f, 0.0f))
        val scale = JsonFields.floats(transform, "scale", floatArrayOf(1.0f, 1.0f))
        return TextureBinding(
            JsonFields.int(value, "index"),
            JsonFields.int(transform, "texCoord", JsonFields.int(value, "texCoord", 0)),
            offset.getOrElse(0) { 0.0f },
            offset.getOrElse(1) { 0.0f },
            scale.getOrElse(0) { 1.0f },
            scale.getOrElse(1) { 1.0f },
            JsonFields.float(transform, "rotation")
        )
    }

    private fun parseMeshes(
        root: JsonValue,
        decoder: AccessorDecoder,
        lodPolicy: LodPolicy,
        variantCount: Int
    ): Array<GltfMesh> {
        val meshes = JsonFields.value(root, "meshes") ?: return emptyArray()
        return Array(meshes.size()) { meshIndex ->
            val mesh = meshes[meshIndex]
            val primitives = JsonFields.value(mesh, "primitives") ?: error("mesh has no primitives")
            GltfMesh(
                JsonFields.string(mesh, "name", "mesh_$meshIndex"),
                Array(primitives.size()) { primitiveIndex ->
                    parsePrimitive(primitives[primitiveIndex], decoder, lodPolicy, variantCount)
                },
                JsonFields.floats(mesh, "weights")
            )
        }
    }

    private fun parsePrimitive(
        value: JsonValue,
        decoder: AccessorDecoder,
        lodPolicy: LodPolicy,
        variantCount: Int
    ): GltfPrimitive {
        val attributes = JsonFields.value(value, "attributes") ?: error("primitive attributes missing")
        val positionAccessor = JsonFields.int(attributes, "POSITION")
        require(positionAccessor >= 0)
        val positions = decoder.readFloats(positionAccessor)
        val vertexCount = decoder.count(positionAccessor)
        val normals = attributeFloats(attributes, "NORMAL", decoder)
        val tangents = attributeFloats(attributes, "TANGENT", decoder)
        val uv0 = attributeFloats(attributes, "TEXCOORD_0", decoder)
        val uv1 = attributeFloats(attributes, "TEXCOORD_1", decoder)
        val colors = attributeFloats(attributes, "COLOR_0", decoder)
        val vertices = ByteBuffer.allocate(vertexCount * VertexLayout.STRIDE).order(ByteOrder.nativeOrder())
        for (vertex in 0 until vertexCount) {
            put3(vertices, positions, vertex * 3, 0.0f, 0.0f, 0.0f)
            put3(vertices, normals, vertex * 3, 0.0f, 1.0f, 0.0f)
            put4(vertices, tangents, vertex * 4, 1.0f, 0.0f, 0.0f, 1.0f)
            put2(vertices, uv0, vertex * 2)
            put2(vertices, uv1, vertex * 2)
            val colorComponents = if (vertexCount == 0) 4 else colors.size / vertexCount
            val colorBase = vertex * colorComponents
            vertices.put(((colors.getOrNull(colorBase) ?: 1.0f) * 255.0f).toInt().coerceIn(0, 255).toByte())
            vertices.put(((colors.getOrNull(colorBase + 1) ?: 1.0f) * 255.0f).toInt().coerceIn(0, 255).toByte())
            vertices.put(((colors.getOrNull(colorBase + 2) ?: 1.0f) * 255.0f).toInt().coerceIn(0, 255).toByte())
            vertices.put(((if (colorComponents == 4) colors.getOrNull(colorBase + 3) else 1.0f) ?: 1.0f).times(255.0f).toInt().coerceIn(0, 255).toByte())
        }
        vertices.flip()
        val joints = attributeInts(attributes, "JOINTS_0", decoder)
        val weights = attributeFloats(attributes, "WEIGHTS_0", decoder)
        val skin = if (joints.isNotEmpty() && weights.isNotEmpty()) {
            ByteBuffer.allocate(vertexCount * VertexLayout.SKIN_STRIDE).order(ByteOrder.nativeOrder()).apply {
                for (vertex in 0 until vertexCount) {
                    val base = vertex * 4
                    for (component in 0 until 4) putShort(joints.getOrElse(base + component) { 0 }.coerceIn(0, 65535).toShort())
                    for (component in 0 until 4) putShort((weights.getOrElse(base + component) { 0.0f } * 65535.0f).toInt().coerceIn(0, 65535).toShort())
                }
                flip()
            }
        } else {
            null
        }
        val sourceIndices = JsonFields.int(value, "indices").let { accessor ->
            if (accessor >= 0) decoder.readInts(accessor) else IntArray(vertexCount) { it }
        }
        val modeCode = JsonFields.int(value, "mode", 4)
        val indices = convertIndices(modeCode, sourceIndices)
        val mode = primitiveMode(modeCode)
        val lodIndices = if (mode == PrimitiveMode.TRIANGLES) {
            MeshLodBuilder.build(indices, positions, lodPolicy.triangleRatios)
        } else {
            arrayOf(indices)
        }
        val targets = JsonFields.value(value, "targets")
        val targetCount = targets?.size() ?: 0
        val morphPositions = FloatArray(targetCount * vertexCount * 3)
        val morphNormals = FloatArray(targetCount * vertexCount * 3)
        if (targets != null) {
            for (target in 0 until targetCount) {
                val accessor = JsonFields.int(targets[target], "POSITION")
                if (accessor >= 0) {
                    val values = decoder.readFloats(accessor)
                    values.copyInto(morphPositions, target * vertexCount * 3, 0, values.size.coerceAtMost(vertexCount * 3))
                }
                val normalAccessor = JsonFields.int(targets[target], "NORMAL")
                if (normalAccessor >= 0) {
                    val values = decoder.readFloats(normalAccessor)
                    values.copyInto(morphNormals, target * vertexCount * 3, 0, values.size.coerceAtMost(vertexCount * 3))
                }
            }
        }
        val min = decoder.min(positionAccessor)
        val max = decoder.max(positionAccessor)
        val bounds = if (min.size >= 3 && max.size >= 3) {
            floatArrayOf(min[0], min[1], min[2], max[0], max[1], max[2])
        } else {
            computeBounds(positions)
        }
        val variantMappings = JsonFields.value(
            JsonFields.value(value, "extensions"),
            "KHR_materials_variants"
        )
        val materialMappings = IntArray(variantCount) { -1 }
        val mappings = JsonFields.value(variantMappings, "mappings")
        if (mappings != null) {
            for (mappingIndex in 0 until mappings.size()) {
                val mapping = mappings[mappingIndex]
                val material = JsonFields.int(mapping, "material")
                if (material < 0) continue
                for (variant in JsonFields.ints(mapping, "variants")) {
                    if (variant in materialMappings.indices) materialMappings[variant] = material
                }
            }
        }
        return GltfPrimitive(
            vertices,
            skin,
            lodIndices,
            vertexCount,
            JsonFields.int(value, "material", 0),
            materialMappings,
            mode,
            bounds,
            morphPositions,
            morphNormals,
            targetCount
        )
    }

    private fun parseMaterialVariantNames(root: JsonValue): Array<String> {
        val variants = JsonFields.value(
            JsonFields.value(root, "extensions"),
            "KHR_materials_variants"
        )
        val names = JsonFields.value(variants, "variants") ?: return emptyArray()
        return Array(names.size()) { index ->
            JsonFields.string(names[index], "name", "variant_$index")
        }
    }

    private fun parseNodes(root: JsonValue, decoder: AccessorDecoder): Array<GltfNode> {
        val values = JsonFields.value(root, "nodes") ?: return emptyArray()
        return Array(values.size()) { index ->
            val node = values[index]
            val punctualLight = JsonFields.value(JsonFields.value(node, "extensions"), "KHR_lights_punctual")
            val instancing = JsonFields.value(JsonFields.value(node, "extensions"), "EXT_mesh_gpu_instancing")
            val attributes = JsonFields.value(instancing, "attributes")
            val translations = attributes?.let { attributeFloats(it, "TRANSLATION", decoder) } ?: FloatArray(0)
            val rotations = attributes?.let { attributeFloats(it, "ROTATION", decoder) } ?: FloatArray(0)
            val scales = attributes?.let { attributeFloats(it, "SCALE", decoder) } ?: FloatArray(0)
            val instanceCount = maxOf(translations.size / 3, rotations.size / 4, scales.size / 3)
            val instanceMatrices = FloatArray(instanceCount * 16)
            for (instance in 0 until instanceCount) {
                val translation = if (translations.isNotEmpty()) instance * 3 else 0
                val rotation = if (rotations.isNotEmpty()) instance * 4 else 0
                val scale = if (scales.isNotEmpty()) instance * 3 else 0
                Matrix4f()
                    .translationRotateScale(
                        translations.getOrElse(translation) { 0.0f },
                        translations.getOrElse(translation + 1) { 0.0f },
                        translations.getOrElse(translation + 2) { 0.0f },
                        rotations.getOrElse(rotation) { 0.0f },
                        rotations.getOrElse(rotation + 1) { 0.0f },
                        rotations.getOrElse(rotation + 2) { 0.0f },
                        rotations.getOrElse(rotation + 3) { 1.0f },
                        scales.getOrElse(scale) { 1.0f },
                        scales.getOrElse(scale + 1) { 1.0f },
                        scales.getOrElse(scale + 2) { 1.0f }
                    )
                    .get(instanceMatrices, instance * 16)
            }
            GltfNode(
                JsonFields.string(node, "name", "node_$index"),
                -1,
                JsonFields.ints(node, "children"),
                JsonFields.int(node, "mesh"),
                JsonFields.int(node, "skin"),
                JsonFields.int(node, "camera"),
                JsonFields.int(punctualLight, "light"),
                instanceMatrices,
                JsonFields.floats(node, "translation", floatArrayOf(0.0f, 0.0f, 0.0f)),
                JsonFields.floats(node, "rotation", floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)),
                JsonFields.floats(node, "scale", floatArrayOf(1.0f, 1.0f, 1.0f)),
                JsonFields.value(node, "matrix")?.let { JsonFields.floats(node, "matrix") },
                JsonFields.floats(node, "weights")
            )
        }
    }

    private fun parseSkins(root: JsonValue, decoder: AccessorDecoder): Array<GltfSkin> {
        val values = JsonFields.value(root, "skins") ?: return emptyArray()
        return Array(values.size()) { index ->
            val skin = values[index]
            val joints = JsonFields.ints(skin, "joints")
            val accessor = JsonFields.int(skin, "inverseBindMatrices")
            val matrices = if (accessor >= 0) {
                decoder.readFloats(accessor)
            } else {
                FloatArray(joints.size * 16).also { output ->
                    for (joint in joints.indices) {
                        output[joint * 16] = 1.0f
                        output[joint * 16 + 5] = 1.0f
                        output[joint * 16 + 10] = 1.0f
                        output[joint * 16 + 15] = 1.0f
                    }
                }
            }
            GltfSkin(JsonFields.string(skin, "name", "skin_$index"), joints, matrices)
        }
    }

    private fun parseCameras(root: JsonValue): Array<GltfCamera> {
        val values = JsonFields.value(root, "cameras") ?: return emptyArray()
        return Array(values.size()) { index ->
            val value = values[index]
            val perspective = JsonFields.value(value, "perspective")
            val orthographic = JsonFields.value(value, "orthographic")
            GltfCamera(
                JsonFields.string(value, "name", "camera_$index"),
                cameraType(JsonFields.string(value, "type", "perspective")),
                JsonFields.float(perspective, "yfov", 0.7853982f),
                JsonFields.float(perspective, "znear", 0.01f),
                JsonFields.float(perspective, "zfar", -1.0f),
                JsonFields.float(perspective, "aspectRatio", -1.0f),
                JsonFields.float(orthographic, "xmag", -1.0f),
                JsonFields.float(orthographic, "ymag", -1.0f)
            )
        }
    }

    private fun parseLights(root: JsonValue): Array<GltfLight> {
        val lights = JsonFields.value(
            JsonFields.value(root, "extensions"),
            "KHR_lights_punctual"
        )
        val values = JsonFields.value(lights, "lights") ?: return emptyArray()
        return Array(values.size()) { index ->
            val value = values[index]
            val spot = JsonFields.value(value, "spot")
            GltfLight(
                JsonFields.string(value, "name", "light_$index"),
                lightType(JsonFields.string(value, "type")),
                JsonFields.floats(value, "color", floatArrayOf(1.0f, 1.0f, 1.0f)),
                JsonFields.float(value, "intensity", 1.0f),
                JsonFields.float(value, "range", -1.0f),
                JsonFields.float(spot, "innerConeAngle", 0.0f),
                JsonFields.float(spot, "outerConeAngle", 0.7853982f)
            )
        }
    }

    private fun parseAnimations(root: JsonValue, decoder: AccessorDecoder): Array<AnimationClip> {
        val values = JsonFields.value(root, "animations") ?: return emptyArray()
        return Array(values.size()) { animationIndex ->
            val animation = values[animationIndex]
            val samplers = JsonFields.value(animation, "samplers") ?: error("animation samplers missing")
            val channels = JsonFields.value(animation, "channels") ?: error("animation channels missing")
            var duration = 0.0f
            val parsed = ArrayList<AnimationChannel>(channels.size())
            for (channelIndex in 0 until channels.size()) {
                val channel = channels[channelIndex]
                val sampler = samplers[JsonFields.int(channel, "sampler")]
                val target = JsonFields.value(channel, "target") ?: error("animation target missing")
                val input = decoder.readFloats(JsonFields.int(sampler, "input"))
                val output = decoder.readFloats(JsonFields.int(sampler, "output"))
                if (input.isNotEmpty()) duration = maxOf(duration, input[input.lastIndex])
                val interpolation = interpolation(JsonFields.string(sampler, "interpolation", "LINEAR"))
                val multiplier = if (interpolation == Interpolation.CUBIC_SPLINE) 3 else 1
                val components = if (input.isEmpty()) 0 else output.size / input.size / multiplier
                val materialTarget = materialUvTarget(target)
                val materialFactorIndex = materialFactorTarget(target)
                if (materialTarget != null) {
                    parsed += AnimationChannel(
                        -1,
                        AnimationPath.MATERIAL_UV,
                        interpolation,
                        input,
                        output,
                        components,
                        materialTarget.materialIndex,
                        materialTarget.textureSlot,
                        materialTarget.textureProperty
                    )
                } else if (materialFactorIndex >= 0) {
                    parsed += AnimationChannel(
                        -1,
                        AnimationPath.MATERIAL_FACTOR,
                        interpolation,
                        input,
                        output,
                        components,
                        materialFactorIndex
                    )
                } else if (!hasAnimationPointer(target)) {
                    parsed += AnimationChannel(
                        JsonFields.int(target, "node"),
                        animationPath(JsonFields.string(target, "path")),
                        interpolation,
                        input,
                        output,
                        components
                    )
                }
            }
            AnimationClip(
                JsonFields.string(animation, "name", "animation_$animationIndex"),
                duration,
                parsed.toTypedArray()
            )
        }
    }

    private fun hasAnimationPointer(target: JsonValue): Boolean =
        JsonFields.value(JsonFields.value(target, "extensions"), "KHR_animation_pointer") != null

    private fun materialUvTarget(target: JsonValue): MaterialUvTarget? {
        val extension = JsonFields.value(JsonFields.value(target, "extensions"), "KHR_animation_pointer")
        val pointer = JsonFields.string(extension, "pointer")
        if (pointer.isEmpty()) return null
        val segments = pointer.split('/')
        if (segments.size < 7 || segments[1] != "materials") return null
        val materialIndex = segments[2].toIntOrNull() ?: return null
        val textureSlot = when (segments.getOrNull(4)) {
            "baseColorTexture" -> 0
            else -> return null
        }
        val transform = segments.getOrNull(5)
        val property = if (transform == "extensions" && segments.getOrNull(6) == "KHR_texture_transform") {
            segments.getOrNull(7)
        } else if (transform == "KHR_texture_transform") {
            segments.getOrNull(6)
        } else {
            null
        }
        val textureProperty = when (property) {
            "offset" -> 0
            "rotation" -> 1
            "scale" -> 2
            else -> return null
        }
        return MaterialUvTarget(materialIndex, textureSlot, textureProperty)
    }

    private fun materialFactorTarget(target: JsonValue): Int {
        val extension = JsonFields.value(JsonFields.value(target, "extensions"), "KHR_animation_pointer")
        val pointer = JsonFields.string(extension, "pointer")
        if (pointer.isEmpty()) return -1
        val segments = pointer.split('/')
        if (
            segments.size != 5 ||
            segments[1] != "materials" ||
            segments[3] != "pbrMetallicRoughness" ||
            segments[4] != "baseColorFactor"
        ) {
            return -1
        }
        return segments[2].toIntOrNull() ?: -1
    }

    private fun parentIndices(nodes: Array<GltfNode>): IntArray {
        val parents = IntArray(nodes.size) { -1 }
        for (index in nodes.indices) {
            for (child in nodes[index].children) {
                require(child in nodes.indices)
                require(parents[child] == -1)
                parents[child] = index
            }
        }
        return parents
    }

    private fun sceneRoots(root: JsonValue, parents: IntArray): IntArray {
        val scenes = JsonFields.value(root, "scenes")
        if (scenes != null && scenes.size() > 0) {
            val scene = JsonFields.int(root, "scene", 0).coerceIn(0, scenes.size() - 1)
            val roots = JsonFields.ints(scenes[scene], "nodes")
            if (roots.isNotEmpty()) return roots
        }
        return parents.indices.filter { parents[it] < 0 }.toIntArray()
    }

    private fun markSceneMask(root: Int, mask: BooleanArray, nodes: Array<GltfNode>) {
        val stack = IntArray(nodes.size)
        var size = 0
        stack[size++] = root
        while (size > 0) {
            val node = stack[--size]
            mask[node] = true
            for (child in nodes[node].children) stack[size++] = child
        }
    }

    private fun topologicalOrder(nodes: Array<GltfNode>, roots: IntArray): IntArray {
        val result = IntArray(nodes.size)
        val stack = IntArray(nodes.size)
        val visited = BooleanArray(nodes.size)
        var size = 0
        for (root in roots.reversed()) stack[size++] = root
        var output = 0
        while (size > 0) {
            val node = stack[--size]
            if (visited[node]) continue
            visited[node] = true
            result[output++] = node
            val children = nodes[node].children
            for (index in children.indices.reversed()) stack[size++] = children[index]
        }
        return result.copyOf(output)
    }

    private fun attributeFloats(attributes: JsonValue, name: String, decoder: AccessorDecoder): FloatArray {
        val accessor = JsonFields.int(attributes, name)
        return if (accessor >= 0) decoder.readFloats(accessor) else FloatArray(0)
    }

    private fun attributeInts(attributes: JsonValue, name: String, decoder: AccessorDecoder): IntArray {
        val accessor = JsonFields.int(attributes, name)
        return if (accessor >= 0) decoder.readInts(accessor) else IntArray(0)
    }

    private fun put2(buffer: ByteBuffer, values: FloatArray, offset: Int) {
        buffer.putFloat(values.getOrElse(offset) { 0.0f })
        buffer.putFloat(values.getOrElse(offset + 1) { 0.0f })
    }

    private fun put3(buffer: ByteBuffer, values: FloatArray, offset: Int, x: Float, y: Float, z: Float) {
        buffer.putFloat(values.getOrElse(offset) { x })
        buffer.putFloat(values.getOrElse(offset + 1) { y })
        buffer.putFloat(values.getOrElse(offset + 2) { z })
    }

    private fun put4(buffer: ByteBuffer, values: FloatArray, offset: Int, x: Float, y: Float, z: Float, w: Float) {
        buffer.putFloat(values.getOrElse(offset) { x })
        buffer.putFloat(values.getOrElse(offset + 1) { y })
        buffer.putFloat(values.getOrElse(offset + 2) { z })
        buffer.putFloat(values.getOrElse(offset + 3) { w })
    }

    private fun primitiveMode(mode: Int): PrimitiveMode = when (mode) {
        0 -> PrimitiveMode.POINTS
        1, 2, 3 -> PrimitiveMode.LINES
        4, 5, 6 -> PrimitiveMode.TRIANGLES
        else -> error("unsupported primitive mode $mode")
    }

    private fun convertIndices(mode: Int, source: IntArray): IntArray = when (mode) {
        0, 1, 4 -> source
        2 -> IntArray(source.size * 2) { index ->
            val edge = index / 2
            if (index and 1 == 0) source[edge] else source[(edge + 1) % source.size]
        }
        3 -> IntArray((source.size - 1).coerceAtLeast(0) * 2) { index ->
            val edge = index / 2
            if (index and 1 == 0) source[edge] else source[edge + 1]
        }
        5 -> IntArray((source.size - 2).coerceAtLeast(0) * 3) { index ->
            val triangle = index / 3
            val corner = index % 3
            if (triangle and 1 == 0) source[triangle + corner] else source[triangle + 2 - corner]
        }
        6 -> IntArray((source.size - 2).coerceAtLeast(0) * 3) { index ->
            val triangle = index / 3
            when (index % 3) {
                0 -> source[0]
                1 -> source[triangle + 1]
                else -> source[triangle + 2]
            }
        }
        else -> error("unsupported primitive mode $mode")
    }

    private fun computeBounds(positions: FloatArray): FloatArray {
        if (positions.isEmpty()) return floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        var index = 0
        while (index + 2 < positions.size) {
            val x = positions[index]
            val y = positions[index + 1]
            val z = positions[index + 2]
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            minZ = minOf(minZ, z)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
            maxZ = maxOf(maxZ, z)
            index += 3
        }
        return floatArrayOf(minX, minY, minZ, maxX, maxY, maxZ)
    }

    private fun assetBounds(meshes: Array<GltfMesh>): FloatArray {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (mesh in meshes) {
            for (primitive in mesh.primitives) {
                val bounds = primitive.bounds
                minX = minOf(minX, bounds[0])
                minY = minOf(minY, bounds[1])
                minZ = minOf(minZ, bounds[2])
                maxX = maxOf(maxX, bounds[3])
                maxY = maxOf(maxY, bounds[4])
                maxZ = maxOf(maxZ, bounds[5])
            }
        }
        return if (minX.isFinite()) floatArrayOf(minX, minY, minZ, maxX, maxY, maxZ) else floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    }

    private fun defaultMaterial(): GltfMaterial = GltfMaterial(
        "default",
        floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
        null,
        1.0f,
        1.0f,
        null,
        null,
        1.0f,
        null,
        1.0f,
        null,
        floatArrayOf(0.0f, 0.0f, 0.0f),
        1.0f,
        AlphaMode.OPAQUE,
        0.5f,
        false,
        false,
        null,
        null,
        null,
        0.0f,
        null,
        0.0f,
        null,
        Float.POSITIVE_INFINITY,
        floatArrayOf(1.0f, 1.0f, 1.0f),
        1.5f,
        0.0f,
        null,
        null
    )

    private fun alphaMode(value: String): AlphaMode = when (value) {
        "MASK" -> AlphaMode.MASK
        "BLEND" -> AlphaMode.BLEND
        else -> AlphaMode.OPAQUE
    }

    private fun cameraType(value: String): CameraType = when (value) {
        "orthographic" -> CameraType.ORTHOGRAPHIC
        else -> CameraType.PERSPECTIVE
    }

    private fun lightType(value: String): LightType = when (value) {
        "spot" -> LightType.SPOT
        "directional" -> LightType.DIRECTIONAL
        else -> LightType.POINT
    }

    private fun animationPath(value: String): AnimationPath = when (value) {
        "translation" -> AnimationPath.TRANSLATION
        "rotation" -> AnimationPath.ROTATION
        "scale" -> AnimationPath.SCALE
        "weights" -> AnimationPath.WEIGHTS
        else -> error("unsupported animation path $value")
    }

    private fun interpolation(value: String): Interpolation = when (value) {
        "STEP" -> Interpolation.STEP
        "CUBICSPLINE" -> Interpolation.CUBIC_SPLINE
        else -> Interpolation.LINEAR
    }

    private fun wrap(value: Int): TextureWrap = when (value) {
        33071 -> TextureWrap.CLAMP_TO_EDGE
        33648 -> TextureWrap.MIRRORED_REPEAT
        else -> TextureWrap.REPEAT
    }

    private fun mimeFromUri(uri: String): String = when {
        uri.endsWith(".jpg", true) || uri.endsWith(".jpeg", true) -> "image/jpeg"
        uri.endsWith(".webp", true) -> "image/webp"
        else -> "image/png"
    }
}
