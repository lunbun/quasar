package io.github.lunbun.pulsar.component;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.IntBuffer;

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
}
