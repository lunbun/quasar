package io.github.lunbun.pulsar.component.uniform;

import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.struct.uniform.LayoutData;
import io.github.lunbun.pulsar.util.shader.ShaderType;
import io.github.lunbun.pulsar.util.uniform.DescriptorSetType;
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

        public DescriptorSetLayout createDescriptorSetLayout(int binding, DescriptorSetType type, ShaderType stage) {
            return createDescriptorSetLayout(new LayoutData[] { new LayoutData(binding, type, stage) });
        }

        public DescriptorSetLayout createDescriptorSetLayout(LayoutData[] layoutData) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkDescriptorSetLayoutBinding.Buffer pLayoutBindings = VkDescriptorSetLayoutBinding.callocStack(layoutData.length, stack);
                for (int i = 0; i < layoutData.length; ++i) {
                    LayoutData data = layoutData[i];

                    VkDescriptorSetLayoutBinding layoutBinding = pLayoutBindings.get(i);
                    layoutBinding.binding(data.binding);
                    layoutBinding.descriptorType(data.type.vulkan);
                    layoutBinding.descriptorCount(1);
                    layoutBinding.pImmutableSamplers(null);
                    layoutBinding.stageFlags(data.stage.bits);
                }

                VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.callocStack(stack);
                layoutInfo.sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
                layoutInfo.pBindings(pLayoutBindings);

                LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

                if (VK10.vkCreateDescriptorSetLayout(this.device.device, layoutInfo, null, pDescriptorSetLayout) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create descriptor set layout!");
                }

                return new DescriptorSetLayout(this.device, pDescriptorSetLayout.get(0));
            }
        }
    }
}
