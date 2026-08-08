package com.micheanl.libgltf.model

/**
 * libgltf · GltfCamera
 *
 * <pre><code>
 * GltfCamera(
 * JsonFields.string(value, "name", "camera_$index"),
 * cameraType(JsonFields.string(value, "type", "perspective")),
 * JsonFields.float(perspective, "yfov", 0.7853982f),
 * JsonFields.float(perspective, "znear", 0.01f),
 * JsonFields.float(perspective, "zfar", -1.0f),
 * JsonFields.float(perspective, "aspectRatio", -1.0f),
 * JsonFields.float(orthographic, "xmag", -1.0f),
 * JsonFields.float(orthographic, "ymag", -1.0f)
 * )
 * </code></pre>
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */


data class GltfCamera(
    val name: String,
    val type: CameraType,
    val yFov: Float,
    val zNear: Float,
    val zFar: Float,
    val aspectRatio: Float,
    val xMag: Float,
    val yMag: Float
)
