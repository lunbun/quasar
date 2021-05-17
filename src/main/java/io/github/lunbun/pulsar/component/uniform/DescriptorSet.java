package io.github.lunbun.pulsar.component.uniform;

import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.vertex.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public final class DescriptorSet {
    private final LogicalDevice device;
    public final long descriptorSet;

    protected DescriptorSet(LogicalDevice device, long descriptorSet) {
        this.device = device;
        this.descriptorSet = descriptorSet;
    }

    public void configure(Buffer buffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.callocStack(1, stack);
            bufferInfo.buffer(buffer.buffer);
            bufferInfo.offset(0);
            bufferInfo.range(buffer.size);

            VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.callocStack(1, stack);
            descriptorWrite.sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            descriptorWrite.dstSet(this.descriptorSet);
            descriptorWrite.dstBinding(0);
            descriptorWrite.dstArrayElement(0);
            descriptorWrite.descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            descriptorWrite.descriptorCount(1);
            descriptorWrite.pBufferInfo(bufferInfo);

            VK10.vkUpdateDescriptorSets(this.device.device, descriptorWrite, null);
        }
    }
}
