package io.github.lunbun.pulsar.component.presentation;

import io.github.lunbun.pulsar.component.drawing.Framebuffer;
import io.github.lunbun.pulsar.component.setup.LogicalDevice;
import io.github.lunbun.pulsar.component.setup.PhysicalDevice;
import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import io.github.lunbun.pulsar.util.PulsarSettings;
import io.github.lunbun.pulsar.util.vulkan.DeviceUtils;
import io.github.lunbun.pulsar.util.misc.MathUtils;
import io.github.lunbun.pulsar.util.vulkan.QueueFamilyIndices;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public final class SwapChain {
    public final PhysicalDevice physicalDevice;
    public final LogicalDevice device;
    public final WindowSurface surface;
    public final long window;
    public final GraphicsCardPreference preference;

    public List<Long> images;
    public int imageFormat;
    public VkExtent2D extent;
    public long swapChain;

    protected SwapChain(LogicalDevice device, PhysicalDevice physicalDevice, WindowSurface surface, long window, GraphicsCardPreference preference) {
        this.device = device;
        this.physicalDevice = physicalDevice;
        this.surface = surface;
        this.window = window;
        this.preference = preference;

        this.images = null;
        this.imageFormat = 0;
        this.extent = null;
        this.swapChain = 0;
    }

    public void destroy() {
        KHRSwapchain.vkDestroySwapchainKHR(this.device.device, this.swapChain, null);
    }

    public void create() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Support.SwapChainSupportDetails swapChainSupport = Support.querySwapChainSupport(this.physicalDevice.device,
                    this.surface, stack);

            VkSurfaceFormatKHR surfaceFormat = Builder.chooseSwapSurfaceFormat(swapChainSupport.formats);
            int presentMode = Builder.chooseSwapPresentMode(swapChainSupport.presentModes);
            VkExtent2D extent = Builder.chooseSwapExtent(swapChainSupport.capabilities, this.window);

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

            QueueFamilyIndices indices = DeviceUtils.findQueueFamilies(this.physicalDevice.device, this.surface.surface, preference);

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

            if (KHRSwapchain.vkCreateSwapchainKHR(this.device.device, createInfo, null, pSwapChain) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain!");
            }

            long swapChain = pSwapChain.get(0);

            KHRSwapchain.vkGetSwapchainImagesKHR(this.device.device, swapChain, pImageCount, null);
            LongBuffer pSwapChainImages = stack.mallocLong(pImageCount.get(0));
            KHRSwapchain.vkGetSwapchainImagesKHR(this.device.device, swapChain, pImageCount, pSwapChainImages);

            List<Long> images = new LongArrayList(pImageCount.get(0));
            for (int i = 0; i < pSwapChainImages.capacity(); ++i) {
                images.add(pSwapChainImages.get(i));
            }

            int imageFormat = surfaceFormat.format();
            VkExtent2D extentCopy = VkExtent2D.create().set(extent);

            this.images = images;
            this.imageFormat = imageFormat;
            this.extent = extentCopy;
            this.swapChain = swapChain;
        }
    }

    public static final class Builder {
        private static VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
            return availableFormats.stream()
                    .filter(availableFormat -> availableFormat.format() == VK10.VK_FORMAT_B8G8R8A8_SRGB)
                    .filter(availableFormat -> availableFormat.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                    .findAny()
                    .orElse(availableFormats.get(0));
        }

        private static int chooseSwapPresentMode(IntBuffer availablePresentModes) {
            int preferredMode = PulsarSettings.PREFER_VSYNC ? KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR :
                    KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR;

            for(int i = 0; i < availablePresentModes.capacity(); i++) {
                if (availablePresentModes.get(i) == preferredMode) {
                    return availablePresentModes.get(i);
                }
            }

            return KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
        }

        private static VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities, long window) {
            if (capabilities.currentExtent().width() != MathUtils.UINT32_MAX) {
                return capabilities.currentExtent();
            } else {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer width = stack.ints(0);
                    IntBuffer height = stack.ints(0);
                    GLFW.glfwGetFramebufferSize(window, width, height);

                    VkExtent2D actualExtent = VkExtent2D.mallocStack().set(width.get(0), height.get(0));

                    actualExtent.width(MathUtils.clamp(capabilities.minImageExtent().width(),
                            capabilities.maxImageExtent().width(), actualExtent.width()));
                    actualExtent.height(MathUtils.clamp(capabilities.minImageExtent().height(),
                            capabilities.maxImageExtent().height(), actualExtent.height()));

                    return actualExtent;
                }
            }
        }

        public static SwapChain createSwapChain(PhysicalDevice physicalDevice, LogicalDevice device, WindowSurface surface, long window, GraphicsCardPreference preference) {
            SwapChain swapChain = new SwapChain(device, physicalDevice, surface, window, preference);
            swapChain.create();
            return swapChain;
        }
    }

    public static class Support {
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
    }
}
