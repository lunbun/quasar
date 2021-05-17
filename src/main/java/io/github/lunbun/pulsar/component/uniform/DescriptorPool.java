package io.github.lunbun.pulsar.component.uniform;

import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.List;

public final class DescriptorPool {
    private final LogicalDevice device;
    public final long descriptorPool;

    public DescriptorPool(LogicalDevice device, int size) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.callocStack(1, stack);
            poolSize.type(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            poolSize.descriptorCount(size);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSize);
            poolInfo.maxSets(size);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if (VK10.vkCreateDescriptorPool(device.device, poolInfo, null, pDescriptorPool) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool!");
            }

            this.descriptorPool = pDescriptorPool.get(0);
            this.device = device;
        }
    }

    public void allocateSets(int count, List<DescriptorSet> sets, DescriptorSetLayout layout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(this.descriptorPool);

            LongBuffer pSetLayouts = stack.mallocLong(count);
            for (int i = 0; i < count; ++i) {
                pSetLayouts.put(layout.layout);
            }

            allocInfo.pSetLayouts(pSetLayouts);

            LongBuffer pDescriptorSets = stack.mallocLong(count);
            if (VK10.vkAllocateDescriptorSets(this.device.device, allocInfo, pDescriptorSets) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor sets!");
            }

            for (int i = 0; i < count; ++i) {
                sets.add(new DescriptorSet(this.device, pDescriptorSets.get(i)));
            }
        }
    }

    public DescriptorSet allocateSet(DescriptorSetLayout layout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
            allocInfo.sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(this.descriptorPool);

            LongBuffer pSetLayouts = stack.longs(layout.layout);

            allocInfo.pSetLayouts(pSetLayouts);

            LongBuffer pDescriptorSets = stack.mallocLong(1);
            if (VK10.vkAllocateDescriptorSets(this.device.device, allocInfo, pDescriptorSets) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor sets!");
            }

            return new DescriptorSet(this.device, pDescriptorSets.get(0));
        }
    }

    public void destroy() {
        VK10.vkDestroyDescriptorPool(this.device.device, this.descriptorPool, null);
    }
}
