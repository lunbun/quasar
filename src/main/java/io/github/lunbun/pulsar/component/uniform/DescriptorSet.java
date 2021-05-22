package io.github.lunbun.pulsar.component.uniform;

import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.texture.Texture;
import io.github.lunbun.pulsar.component.texture.TextureSampler;
import io.github.lunbun.pulsar.component.vertex.Buffer;
import io.github.lunbun.pulsar.struct.uniform.DescriptorSetConfiguration;
import io.github.lunbun.pulsar.struct.uniform.SamplerConfiguration;
import io.github.lunbun.pulsar.struct.uniform.UniformConfiguration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public final class DescriptorSet {
    private final LogicalDevice device;
    public final long descriptorSet;

    protected DescriptorSet(LogicalDevice device, long descriptorSet) {
        this.device = device;
        this.descriptorSet = descriptorSet;
    }

    public void configure(DescriptorSetConfiguration[] configurations) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.callocStack(configurations.length, stack);

            for (int i = 0; i < configurations.length; ++i) {
                DescriptorSetConfiguration configuration = configurations[i];

                VkWriteDescriptorSet descriptorWrite = descriptorWrites.get(i);
                descriptorWrite.sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                descriptorWrite.dstSet(this.descriptorSet);
                descriptorWrite.dstBinding(configuration.binding);
                descriptorWrite.dstArrayElement(0);
                descriptorWrite.descriptorCount(1);

                if (configuration instanceof UniformConfiguration) {
                    Buffer uniform = ((UniformConfiguration) configuration).uniform;
                    VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.callocStack(1, stack);
                    bufferInfo.buffer(uniform.buffer);
                    bufferInfo.offset(0);
                    bufferInfo.range(uniform.size);
                    descriptorWrite.descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                    descriptorWrite.pBufferInfo(bufferInfo);
                } else if (configuration instanceof SamplerConfiguration) {
                    SamplerConfiguration samplerConfiguration = (SamplerConfiguration) configuration;
                    VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.callocStack(1, stack);
                    imageInfo.imageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                    imageInfo.imageView(samplerConfiguration.texture.imageView);
                    imageInfo.sampler(samplerConfiguration.sampler.sampler);
                    descriptorWrite.descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                    descriptorWrite.pImageInfo(imageInfo);
                }
            }

            VK10.vkUpdateDescriptorSets(this.device.device, descriptorWrites, null);
        }
    }
}
