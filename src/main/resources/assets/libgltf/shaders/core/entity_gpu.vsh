#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:light.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec4 Tangent;
layout(location = 3) in vec2 UV0;
layout(location = 4) in vec2 TexCoord1;
layout(location = 5) in vec4 Color;
layout(location = 6) in vec4 InstanceMatrix0;
layout(location = 7) in vec4 InstanceMatrix1;
layout(location = 8) in vec4 InstanceMatrix2;
layout(location = 9) in vec4 InstanceMatrix3;
layout(location = 10) in vec4 InstanceNormal0;
layout(location = 11) in vec4 InstanceNormal1;
layout(location = 12) in vec4 InstanceNormal2;
layout(location = 13) in vec4 InstanceColor;
layout(location = 14) in ivec2 InstanceLight;
layout(location = 15) in ivec2 InstanceOverlay;
layout(location = 16) in int PaletteOffset;
layout(location = 17) in vec4 InstanceUvTransform0;
layout(location = 18) in vec2 InstanceUvTransform1;
#ifdef SKINNED
layout(location = 19) in uvec4 Joints;
layout(location = 20) in vec4 Weights;
uniform samplerBuffer JointMatrices;
#endif

#if !defined(OIT_ALPHA_ONLY)
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
#endif

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec4 lightMapColor;
layout(location = 4) out vec4 overlayColor;
layout(location = 5) out vec2 texCoord0;

#ifdef SKINNED
mat4 jointMatrix(uint jointIndex) {
    int offset = PaletteOffset + int(jointIndex) * 4;
    return mat4(
        texelFetch(JointMatrices, offset),
        texelFetch(JointMatrices, offset + 1),
        texelFetch(JointMatrices, offset + 2),
        texelFetch(JointMatrices, offset + 3)
    );
}
#endif

void main() {
    vec4 position = vec4(Position, 1.0);
    vec3 normal = Normal;
#ifdef SKINNED
    mat4 skinMatrix =
        jointMatrix(Joints.x) * Weights.x +
        jointMatrix(Joints.y) * Weights.y +
        jointMatrix(Joints.z) * Weights.z +
        jointMatrix(Joints.w) * Weights.w;
    position = skinMatrix * position;
    normal = mat3(skinMatrix) * normal;
#endif
    mat4 instanceMatrix = mat4(InstanceMatrix0, InstanceMatrix1, InstanceMatrix2, InstanceMatrix3);
    mat3 normalMatrix = mat3(InstanceNormal0.xyz, InstanceNormal1.xyz, InstanceNormal2.xyz);
    vec4 worldPosition = instanceMatrix * position;
    vec3 worldNormal = normalize(normalMatrix * normal);
    gl_Position = ProjMat * ModelViewMat * worldPosition;
    sphericalVertexDistance = fog_spherical_distance(worldPosition.xyz);
    cylindricalVertexDistance = fog_cylindrical_distance(worldPosition.xyz);
#ifdef UNLIT
    vertexColor = Color * InstanceColor;
#else
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, worldNormal, Color * InstanceColor);
#endif
#if !defined(OIT_ALPHA_ONLY)
#ifdef UNLIT
    lightMapColor = vec4(1.0);
    overlayColor = vec4(0.0);
#else
    lightMapColor = sample_lightmap(Sampler2, InstanceLight);
    overlayColor = texelFetch(Sampler1, InstanceOverlay, 0);
#endif
#endif
    texCoord0 = InstanceUvTransform0.xy +
        mat2(InstanceUvTransform0.z, InstanceUvTransform1.x, InstanceUvTransform0.w, InstanceUvTransform1.y) * UV0;
}
