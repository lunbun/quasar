package io.github.lunbun.pulsar.component;

import io.github.lunbun.pulsar.struct.GraphicsCardPreference;
import io.github.lunbun.pulsar.struct.QueueFamily;
import io.github.lunbun.pulsar.util.DeviceUtils;
import io.github.lunbun.pulsar.util.MathUtil;
import io.github.lunbun.pulsar.util.QueueFamilyIndices;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public final class SwapChain {
    public static class SwapChainSupportDetails {
        public VkSurfaceCapabilitiesKHR capabilities;
        public VkSurfaceFormatKHR.Buffer formats;
        public IntBuffer presentModes;
    }

    public static SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice physicalDevice, WindowSurface surface, MemoryStack stack) {
        SwapChainSupportDetails details = new SwapChainSupportDetails();

        details.capabilities = VkSurfaceCapabilitiesKHR.mallocStack(stack);
        KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface.surface, details.capabilities);

        IntBuffer count = stack.ints(0);
        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.surface, count, null);

        if (count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.mallocStack(count.get(0), stack);
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.surface, count, details.formats);
        }

        count.put(0, 0);
        KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.surface, count, null);

        if (count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.surface, count, details.presentModes);
        }

        return details;
    }

    public PhysicalDeviceManager physicalDevice;
    public LogicalDeviceManager logicalDevice;
    public WindowSurface surface;
    public GraphicsCardPreference preference;
    public List<Long> images;
    public int imageFormat;
    public VkExtent2D extent;
    public long swapChain;
    public long window;

    private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        return availableFormats.stream()
                .filter(availableFormat -> availableFormat.format() == VK10.VK_FORMAT_B8G8R8A8_SRGB)
                .filter(availableFormat -> availableFormat.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .findAny()
                .orElse(availableFormats.get(0));
    }

    private int chooseSwapPresentMode(IntBuffer availablePresentModes) {
        for(int i = 0;i < availablePresentModes.capacity();i++) {
            if (availablePresentModes.get(i) == KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR) {
                return availablePresentModes.get(i);
            }
        }

        return KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
    }

    private VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities, long window) {
        if (capabilities.currentExtent().width() != MathUtil.UINT32_MAX) {
            return capabilities.currentExtent();
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.ints(0);
                IntBuffer height = stack.ints(0);
                GLFW.glfwGetFramebufferSize(window, width, height);

                VkExtent2D actualExtent = VkExtent2D.mallocStack().set(width.get(0), height.get(0));

                actualExtent.width(MathUtil.clamp(capabilities.minImageExtent().width(),
                        capabilities.maxImageExtent().width(), actualExtent.width()));
                actualExtent.height(MathUtil.clamp(capabilities.minImageExtent().height(),
                        capabilities.maxImageExtent().height(), actualExtent.height()));

                return actualExtent;
            }
        }
    }

    public void createSwapChain() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SwapChainSupportDetails swapChainSupport = querySwapChainSupport(this.physicalDevice.physicalDevice, this.surface, stack);

            VkSurfaceFormatKHR surfaceFormat = this.chooseSwapSurfaceFormat(swapChainSupport.formats);
            int presentMode = this.chooseSwapPresentMode(swapChainSupport.presentModes);
            VkExtent2D extent = this.chooseSwapExtent(swapChainSupport.capabilities, this.window);

            IntBuffer pImageCount = stack.ints(swapChainSupport.capabilities.minImageCount() + 1);
            if ((swapChainSupport.capabilities.maxImageCount() > 0) &&
                    (pImageCount.get(0) > swapChainSupport.capabilities.maxImageCount())) {
                pImageCount.put(0, swapChainSupport.capabilities.maxImageCount());
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);
            createInfo.sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(this.surface.surface);

            createInfo.minImageCount(pImageCount.get(0));
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            QueueFamilyIndices indices = DeviceUtils.findQueueFamilies(this.physicalDevice.physicalDevice,
                    this.surface.surface, this.preference);

            if (!indices.getFamilyIndex(QueueFamily.GRAPHICS).equals(indices.getFamilyIndex(QueueFamily.PRESENT))) {
                createInfo.imageSharingMode(VK10.VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(indices.getFamilyIndex(QueueFamily.GRAPHICS), indices.getFamilyIndex(QueueFamily.PRESENT)));
            } else {
                createInfo.imageSharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(swapChainSupport.capabilities.currentTransform());
            createInfo.compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);

            createInfo.oldSwapchain(VK10.VK_NULL_HANDLE);

            LongBuffer pSwapChain = stack.longs(VK10.VK_NULL_HANDLE);

            if (KHRSwapchain.vkCreateSwapchainKHR(this.logicalDevice.device, createInfo, null, pSwapChain) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain!");
            }

            this.swapChain = pSwapChain.get(0);

            KHRSwapchain.vkGetSwapchainImagesKHR(this.logicalDevice.device, this.swapChain, pImageCount, null);
            LongBuffer pSwapChainImages = stack.mallocLong(pImageCount.get(0));
            KHRSwapchain.vkGetSwapchainImagesKHR(this.logicalDevice.device, this.swapChain, pImageCount, pSwapChainImages);

            this.images = new LongArrayList(pImageCount.get(0));
            for (int i = 0; i < pSwapChainImages.capacity(); ++i) {
                this.images.add(pSwapChainImages.get(i));
            }

            this.imageFormat = surfaceFormat.format();
            this.extent = VkExtent2D.create().set(extent);
        }
    }

    public void destroy() {
        KHRSwapchain.vkDestroySwapchainKHR(this.logicalDevice.device, this.swapChain, null);
    }
}
