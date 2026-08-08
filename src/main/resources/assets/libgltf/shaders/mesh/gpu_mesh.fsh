#version 450
layout(set = 0, binding = PROJECTION_BINDING, std140) uniform Projection { mat4 ProjMat; };
layout(set = 0, binding = DYNAMIC_TRANSFORMS_BINDING, std140) uniform DynamicTransforms { mat4 ModelViewMat; mat4 TextureMat; vec4 ColorModulator; vec3 ModelOffset; };
layout(set = 0, binding = FOG_BINDING, std140) uniform Fog { vec4 FogColor; float FogEnvironmentalStart; float FogEnvironmentalEnd; float FogRenderDistanceStart; float FogRenderDistanceEnd; float FogSkyEnd; float FogCloudsEnd; };
layout(set = 0, binding = SAMPLER0_BINDING) uniform sampler2D Sampler0;
layout(set = 0, binding = SAMPLER1_BINDING) uniform sampler2D Sampler1;
layout(set = 0, binding = SAMPLER2_BINDING) uniform sampler2D Sampler2;
#ifdef OIT
layout(set = 0, binding = DEPTH_BOUNDS_BINDING) uniform sampler2D DepthBoundsSampler;
#ifdef COEFF0_BINDING
layout(set = 0, binding = COEFF0_BINDING) uniform sampler2D Coeff0;
#endif
#if defined(COEFF1_BINDING) && COEFF_COUNT > 4
layout(set = 0, binding = COEFF1_BINDING) uniform sampler2D Coeff1;
#endif
#endif
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 lightMapCoord;
layout(location = 4) in vec2 overlayCoord;
layout(location = 5) in vec2 texCoord0;
#if !defined(OIT_ALPHA_ONLY) || defined(OIT_DEPTH_BOUNDS)
layout(location = 0) out vec4 fragColor;
#endif
#if defined(OIT_TRANSMITTANCE)
layout(location = 0) out vec4 coeff[COEFF_ATTACHMENT_COUNT];
#endif
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
#ifdef OIT
float deviceToLinearDepth(float deviceDepth) {
    #ifndef RENDERPEARL_DEPTH_IS_ZERO_TO_ONE
    deviceDepth = (deviceDepth - 0.5) * 2.0;
    #endif
    #ifdef OIT_FORCE_ZERO_DEPTH
    return 0.0;
    #else
    return ProjMat[3][2] / (deviceDepth + ProjMat[2][2]);
    #endif
}
float toAbsorbance(float transmittance) {
    return clamp(-log(max(transmittance, 0.0001)), 0.0, 4.0);
}
float normalizeDepth(float fragmentDeviceDepth) {
    const float highPrecisionThreshold = 10.0;
    const int lowPrecisionCoeffCount = 2;
    vec4 depthBoundsSample = texelFetch(DepthBoundsSampler, ivec2(gl_FragCoord.xy), 0);
    float closestBoundLinearDepth = -depthBoundsSample.r;
    float furthestBoundLinearDepth = depthBoundsSample.g;
    float fragmentLinearDepth = deviceToLinearDepth(fragmentDeviceDepth);
    float range = max(furthestBoundLinearDepth - closestBoundLinearDepth, 0.0001);
    float depthWithinBounds = clamp(fragmentLinearDepth - closestBoundLinearDepth, 0.0, range);
    float depthFractionWithHighPrecision = float(COEFF_COUNT - lowPrecisionCoeffCount) / float(COEFF_COUNT);
    float depthWithHighPrecision = depthFractionWithHighPrecision * range;
    float mappedDepth;
    if (depthWithHighPrecision <= highPrecisionThreshold) {
        mappedDepth = depthWithinBounds / range;
    } else if (depthWithinBounds <= highPrecisionThreshold) {
        mappedDepth = (depthWithinBounds / highPrecisionThreshold) * depthFractionWithHighPrecision;
    } else {
        mappedDepth = depthFractionWithHighPrecision +
            ((depthWithinBounds - highPrecisionThreshold) / (range - highPrecisionThreshold)) *
            (1.0 - depthFractionWithHighPrecision);
    }
    return mappedDepth * (1.0 - 1.0 / float(COEFF_COUNT));
#endif
#ifdef OIT_DEPTH_BOUNDS
void calculateDepthBounds(float fragmentDeviceDepth, float alpha) {
    float fragmentLinearDepth = deviceToLinearDepth(fragmentDeviceDepth);
    float opaqueFragmentDeviceDepth = alpha > 0.99 ? fragmentDeviceDepth : 0.0;
    fragColor = vec4(-fragmentLinearDepth, fragmentLinearDepth, fragmentDeviceDepth, opaqueFragmentDeviceDepth);
#endif
#ifdef OIT_TRANSMITTANCE
void addTransmittance(float alpha) {
    float transmittance = 1.0 - alpha;
    float absorbance = toAbsorbance(transmittance);
    float depth = normalizeDepth(gl_FragCoord.z);
    float coefficients[COEFF_COUNT];
    for (int i = 0; i < COEFF_COUNT; i++) coefficients[i] = 0.0;
    depth *= float(COEFF_COUNT - 1) / float(COEFF_COUNT);
    int index = clamp(int(floor(depth * float(COEFF_COUNT))), 0, COEFF_COUNT - 1);
    index += COEFF_COUNT - 1;
    for (int i = 0; i < (WAVELET_RANK + 1); i++) {
        int power = 2 - i;
        int newIndex = (index - 1) >> 1;
        float k = float((newIndex + 1) & ((1 << power) - 1));
        int waveletSign = ((index & 1) << 1) - 1;
        float waveletPhase = float((index + 1) & 1) * exp2(-float(power));
        float addend = ((depth - exp2(-float(power)) * k) * float(waveletSign) + waveletPhase) *
            exp2(float(power) * 0.5) * absorbance;
        coefficients[newIndex] = addend;
        index = newIndex;
    }
    coefficients[COEFF_COUNT - 1] = absorbance - (absorbance * depth);
    for (int attachmentIndex = 0; attachmentIndex < COEFF_ATTACHMENT_COUNT; attachmentIndex++) {
        for (int i = 0; i < 4; i++) {
            coeff[attachmentIndex][i] = coefficients[attachmentIndex * 4 + i];
        }
    }
#endif
#ifdef OIT_ACCUMULATE
float evaluateWaveletsCorrected(float coefficients[COEFF_COUNT], float depth, float currentAbsorbance) {
    float scaleCoeff = coefficients[COEFF_COUNT - 1];
    if (scaleCoeff == 0.0) return 0.0;
    scaleCoeff -= (currentAbsorbance * -depth) + currentAbsorbance;
    depth *= float(COEFF_COUNT - 1) / float(COEFF_COUNT);
    float coeffDepth = depth * float(COEFF_COUNT);
    int indexB = clamp(int(floor(coeffDepth)), 0, COEFF_COUNT - 1);
    bool sampleA = indexB >= 1;
    int indexA = sampleA ? (indexB - 1) : indexB;
    indexB += COEFF_COUNT - 1;
    indexA += COEFF_COUNT - 1;
    float b = scaleCoeff;
    float a = sampleA ? scaleCoeff : 0.0;
    for (int i = 0; i < (WAVELET_RANK + 1); i++) {
        int power = 2 - i;
        int newIndexB = (indexB - 1) >> 1;
        int waveletSignB = ((indexB & 1) << 1) - 1;
        float coeffB = coefficients[newIndexB];
        float waveletPhaseB = float((indexB + 1) & 1) * exp2(-float(power));
        float k = float((newIndexB + 1) & ((1 << power) - 1));
        float addend = ((depth - exp2(-float(power)) * k) * float(waveletSignB) + waveletPhaseB) *
            exp2(float(power) * 0.5) * currentAbsorbance;
        coeffB -= addend;
        b -= exp2(float(power) * 0.5) * coeffB * float(waveletSignB);
        indexB = newIndexB;
        if (sampleA) {
            int newIndexA = (indexA - 1) >> 1;
            int waveletSignA = ((indexA & 1) << 1) - 1;
            float coeffA = (newIndexA == newIndexB) ? coeffB : coefficients[newIndexA];
            a -= exp2(float(power) * 0.5) * coeffA * float(waveletSignA);
            indexA = newIndexA;
        }
    }
    float t = coeffDepth >= float(COEFF_COUNT) ? 1.0 : fract(coeffDepth);
    return mix(a, b, t);
}
float sampleTransmittance(ivec2 pos, float depth, float currentAbsorbance) {
    float coefficients[COEFF_COUNT];
    const int targetCount = COEFF_COUNT / 4;
    vec4 coeffSamples[targetCount];
    coeffSamples[0] = texelFetch(Coeff0, pos, 0);
    #if COEFF_COUNT > 4
    coeffSamples[1] = texelFetch(Coeff1, pos, 0);
    #endif
    for (int i = 0; i < targetCount; i++) {
        for (int j = 0; j < 4; j++) {
            coefficients[i * 4 + j] = coeffSamples[i][j];
        }
    }
    return clamp(exp(-evaluateWaveletsCorrected(coefficients, depth, currentAbsorbance)), 0.0001, 1.0);
}
vec4 sampleColorForAccumulation(vec4 color) {
    float transmittance = 1.0 - color.a;
    float absorbance = toAbsorbance(transmittance);
    float accumAlpha = color.a;
    float sampledTransmittance = sampleTransmittance(ivec2(gl_FragCoord.xy), normalizeDepth(gl_FragCoord.z), absorbance);
    return vec4(color.rgb * color.a, accumAlpha) * sampledTransmittance;
}
#endif
void main() {
#ifdef MESH_DEBUG_MINIMAL
#ifdef OIT_TRANSMITTANCE
    coeff[0] = vec4(1.0, 0.0, 0.0, 1.0);
#endif
#if !defined(OIT_ALPHA_ONLY) || defined(OIT_DEPTH_BOUNDS)
    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
#endif
    return;
#endif
#ifdef MESH_DEBUG_FLAT
#ifdef OIT_TRANSMITTANCE
    coeff[0] = vec4(1.0, 0.0, 0.0, 1.0);
#endif
#if !defined(OIT_ALPHA_ONLY) || defined(OIT_DEPTH_BOUNDS)
    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
#endif
    return;
#endif
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    vec4 overlayColor = texelFetch(Sampler1, ivec2(overlayCoord), 0);
    vec4 lightMapColor = texture(Sampler2, lightMapCoord);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) discard;
#endif
#ifdef OIT_ALPHA_ONLY
    #ifdef OIT_DEPTH_BOUNDS
    if (color.a < 0.01) discard;
    calculateDepthBounds(gl_FragCoord.z, color.a);
    #elif defined(OIT_TRANSMITTANCE)
    addTransmittance(color.a);
    #endif
#else
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color.rgb *= lightMapColor.rgb;
    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
    #else
    vec4 fogColor = FogColor;
    #endif
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
#endif
}
