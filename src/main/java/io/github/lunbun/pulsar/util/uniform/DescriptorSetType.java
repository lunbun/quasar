package io.github.lunbun.pulsar.util.uniform;

import org.lwjgl.vulkan.VK10;

public enum DescriptorSetType {
    UNIFORM(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER),
    IMAGE_SAMPLER(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);

    public int vulkan;

    DescriptorSetType(int vulkan) {
        this.vulkan = vulkan;
    }
}
