#version 460
layout(std140, binding = 1) uniform DynamicTransforms { mat4 ModelViewMat; mat4 TextureMat; vec4 ColorModulator; vec3 ModelOffset; };
layout(std140, binding = 2) uniform Fog { vec4 FogColor; float FogEnvironmentalStart; float FogEnvironmentalEnd; float FogRenderDistanceStart; float FogRenderDistanceEnd; float FogSkyEnd; float FogCloudsEnd; };
layout(binding = 0) uniform sampler2D Sampler0;
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec4 lightMapColor;
layout(location = 4) in vec4 overlayColor;
layout(location = 5) in vec2 texCoord0;
layout(location = 0) out vec4 fragColor;
float linear_fog_value(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) return 0.0;
    if (vertexDistance >= fogEnd) return 1.0;
    return (vertexDistance - fogStart) / (fogEnd - fogStart);
}
float total_fog_value(float spherical, float cylindrical, float envStart, float envEnd, float renderStart, float renderEnd) {
    return max(linear_fog_value(spherical, envStart, envEnd), linear_fog_value(cylindrical, renderStart, renderEnd));
}
vec4 apply_fog(vec4 inColor, float spherical, float cylindrical, float envStart, float envEnd, float renderStart, float renderEnd, vec4 fogColor) {
    float value = total_fog_value(spherical, cylindrical, envStart, envEnd, renderStart, renderEnd);
    return vec4(mix(inColor.rgb, fogColor.rgb, value * fogColor.a), inColor.a);
}
void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color.rgb *= lightMapColor.rgb;
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
