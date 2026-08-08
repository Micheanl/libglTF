#version 330

#include <minecraft:light.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec3 Normal;
in vec4 Tangent;
in vec2 UV0;
in vec2 TexCoord1;
in vec4 Color;
in vec4 InstanceMatrix0;
in vec4 InstanceMatrix1;
in vec4 InstanceMatrix2;
in vec4 InstanceMatrix3;
in vec4 InstanceNormal0;
in vec4 InstanceNormal1;
in vec4 InstanceNormal2;
in vec4 InstanceColor;
in ivec2 InstanceLight;
in ivec2 InstanceOverlay;
in int PaletteOffset;
#ifdef SKINNED
in uvec4 Joints;
in vec4 Weights;
uniform samplerBuffer JointMatrices;
#endif

#if !defined(OIT_ALPHA_ONLY)
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
#endif

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;

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
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, worldNormal, Color * InstanceColor);
#if !defined(OIT_ALPHA_ONLY)
    lightMapColor = sample_lightmap(Sampler2, InstanceLight);
    overlayColor = texelFetch(Sampler1, InstanceOverlay, 0);
#endif
    texCoord0 = UV0;
}
