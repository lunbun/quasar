package io.github.lunbun.pulsar.component.presentation;

import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;
import java.util.List;

public final class ImageViewsManager {
    public List<Long> imageViews;
    public LogicalDevice device;

    public static long createImageView(LogicalDevice device, long image, int format) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.callocStack(stack);

            createInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            createInfo.image(image);
            createInfo.viewType(VK10.VK_IMAGE_VIEW_TYPE_2D);
            createInfo.format(format);

            createInfo.components().r(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().g(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().b(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.components().a(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);

            createInfo.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            createInfo.subresourceRange().baseMipLevel(0);
            createInfo.subresourceRange().levelCount(1);
            createInfo.subresourceRange().baseArrayLayer(0);
            createInfo.subresourceRange().layerCount(1);

            LongBuffer pImageView = stack.mallocLong(1);

            if (VK10.vkCreateImageView(device.device, createInfo, null, pImageView) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create image views!");
            }

            return pImageView.get(0);
        }
    }

    public static void destroyImageView(LogicalDevice device, long imageView) {
        VK10.vkDestroyImageView(device.device, imageView, null);
    }

    public void createImageViews(LogicalDevice device, SwapChain swapChain) {
        this.imageViews = new LongArrayList(swapChain.images.size());

        for (long image : swapChain.images) {
            this.imageViews.add(createImageView(device, image, swapChain.imageFormat));
        }

        this.device = device;
    }

    public void destroy() {
        for (int i = this.imageViews.size() - 1; i >= 0; --i) {
            destroyImageView(this.device, this.imageViews.get(i));
            this.imageViews.remove(i);
        }
    }
}
