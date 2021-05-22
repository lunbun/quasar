package io.github.lunbun.pulsar.component.texture;

import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.setup.PhysicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

public final class TextureSampler {
    private final LogicalDevice device;

    public final long sampler;

    protected TextureSampler(LogicalDevice device, long sampler) {
        this.device = device;
        this.sampler = sampler;
    }

    public void destroy() {
        VK10.vkDestroySampler(this.device.device, sampler, null);
    }

    public static final class Builder {
        private final LogicalDevice device;
        private final PhysicalDevice physicalDevice;

        public Builder(LogicalDevice device, PhysicalDevice physicalDevice) {
            this.device = device;
            this.physicalDevice = physicalDevice;
        }

        public TextureSampler createSampler(boolean useLinearFilter) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.callocStack(stack);
                samplerInfo.sType(VK10.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
                samplerInfo.magFilter(useLinearFilter ? VK10.VK_FILTER_LINEAR : VK10.VK_FILTER_NEAREST);
                samplerInfo.minFilter(useLinearFilter ? VK10.VK_FILTER_LINEAR : VK10.VK_FILTER_NEAREST);
                samplerInfo.addressModeU(VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT);
                samplerInfo.addressModeV(VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT);
                samplerInfo.addressModeW(VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT);
                samplerInfo.anisotropyEnable(true);
                samplerInfo.maxAnisotropy(this.physicalDevice.getLimits(stack).maxSamplerAnisotropy());
                samplerInfo.borderColor(VK10.VK_BORDER_COLOR_INT_OPAQUE_BLACK);
                samplerInfo.unnormalizedCoordinates(false);
                samplerInfo.compareEnable(false);
                samplerInfo.compareOp(VK10.VK_COMPARE_OP_ALWAYS);
                samplerInfo.mipmapMode(VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR);
                samplerInfo.mipLodBias(0);
                samplerInfo.minLod(0);
                samplerInfo.maxLod(0);

                LongBuffer pTextureSampler = stack.mallocLong(1);

                if (VK10.vkCreateSampler(this.device.device, samplerInfo, null, pTextureSampler) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create texture sampler!");
                }

                return new TextureSampler(this.device, pTextureSampler.get(0));
            }
        }
    }
}
