package io.github.lunbun.pulsar.component.presentation;

import io.github.lunbun.pulsar.component.setup.LogicalDeviceManager;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;
import java.util.List;

public final class ImageViewsManager {
    public List<Long> imageViews;
    public LogicalDeviceManager logicalDevice;
    public SwapChain swapChain;

    public void createImageViews() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.imageViews = new LongArrayList(this.swapChain.images.size());
            LongBuffer pImageView = stack.longs(0);

            for (long image : this.swapChain.images) {
                VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.callocStack(stack);

                createInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                createInfo.image(image);
                createInfo.viewType(VK10.VK_IMAGE_VIEW_TYPE_2D);
                createInfo.format(this.swapChain.imageFormat);

                createInfo.components().r(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().g(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().b(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().a(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);

                createInfo.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
                createInfo.subresourceRange().baseMipLevel(0);
                createInfo.subresourceRange().levelCount(1);
                createInfo.subresourceRange().baseArrayLayer(0);
                createInfo.subresourceRange().layerCount(1);

                if (VK10.vkCreateImageView(this.logicalDevice.device, createInfo, null, pImageView) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create image views!");
                }

                this.imageViews.add(pImageView.get(0));
            }
        }
    }

    public void destroy() {
        for (Long imageView : this.imageViews) {
            VK10.vkDestroyImageView(this.logicalDevice.device, imageView, null);
        }
    }
}
