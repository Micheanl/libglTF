package com.micheanl.libgltf.mixin;

import com.micheanl.libgltf.render.vulkan.GltfGpuDrivenSettings;
import com.mojang.renderpearl.backend.vulkan.VulkanBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanFeatureSets;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import com.mojang.renderpearl.backend.vulkan.init.VulkanFeature;
import com.mojang.renderpearl.backend.vulkan.init.VulkanPNextStruct;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.vulkan.EXTMeshShader;
import org.lwjgl.vulkan.VkPhysicalDeviceMeshShaderFeaturesEXT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VulkanBackend.class)
public abstract class VulkanBackendMeshShaderMixin {
    private static final VulkanPNextStruct LIBGLTF_MESH_FEATURES = new VulkanPNextStruct(
            VkPhysicalDeviceMeshShaderFeaturesEXT.class,
            EXTMeshShader.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_EXT,
            VkPhysicalDeviceMeshShaderFeaturesEXT.SIZEOF
    );
    private static final VulkanFeature LIBGLTF_TASK_SHADER = new VulkanFeature(
            LIBGLTF_MESH_FEATURES,
            "taskShader",
            VkPhysicalDeviceMeshShaderFeaturesEXT.TASKSHADER
    );
    private static final VulkanFeature LIBGLTF_MESH_SHADER = new VulkanFeature(
            LIBGLTF_MESH_FEATURES,
            "meshShader",
            VkPhysicalDeviceMeshShaderFeaturesEXT.MESHSHADER
    );
    private static final FeatureSet LIBGLTF_MESH_SHADER_FEATURESET = new FeatureSet(
            "libgltf mesh shader",
            Set.of(EXTMeshShader.VK_EXT_MESH_SHADER_EXTENSION_NAME),
            Set.of(LIBGLTF_TASK_SHADER, LIBGLTF_MESH_SHADER)
    );

    @Redirect(
            method = "createDevice",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanFeatureSets;optionalFeatureSets()Ljava/util/Set;"
            ),
            require = 1
    )
    private Set<FeatureSet> libgltf$enableMeshShader(Set<FeatureSet> original) {
        if (!GltfGpuDrivenSettings.INSTANCE.getMeshShader()) {
            return original;
        }
        Set<FeatureSet> featureSets = new HashSet<>(original);
        featureSets.add(LIBGLTF_MESH_SHADER_FEATURESET);
        return featureSets;
    }
}
