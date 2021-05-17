package io.github.lunbun.pulsar.component.uniform;

import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import java.nio.LongBuffer;

public final class DescriptorSetLayout {
    public final long layout;

    private final LogicalDevice device;

    protected DescriptorSetLayout(LogicalDevice device, long layout) {
        this.layout = layout;
        this.device = device;
    }

    public void destroy() {
        VK10.vkDestroyDescriptorSetLayout(this.device.device, this.layout, null);
    }

    public static final class Builder {
        private final LogicalDevice device;

        public Builder(LogicalDevice device) {
            this.device = device;
        }

        public DescriptorSetLayout createDescriptorSetLayout() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkDescriptorSetLayoutBinding.Buffer uboLayoutBinding = VkDescriptorSetLayoutBinding.callocStack(1, stack);
                uboLayoutBinding.binding(0);
                uboLayoutBinding.descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.pImmutableSamplers(null);
                uboLayoutBinding.stageFlags(VK10.VK_SHADER_STAGE_VERTEX_BIT);

                VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.callocStack(stack);
                layoutInfo.sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
                layoutInfo.pBindings(uboLayoutBinding);

                LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

                if (VK10.vkCreateDescriptorSetLayout(this.device.device, layoutInfo, null, pDescriptorSetLayout) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create descriptor set layout!");
                }

                return new DescriptorSetLayout(this.device, pDescriptorSetLayout.get(0));
            }
        }
    }
}
