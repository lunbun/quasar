package io.github.lunbun.pulsar.component.setup;

import io.github.lunbun.pulsar.component.presentation.SwapChain;
import io.github.lunbun.pulsar.component.presentation.WindowSurface;
import io.github.lunbun.pulsar.struct.setup.DeviceType;
import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.util.vulkan.DeviceUtils;
import io.github.lunbun.pulsar.util.vulkan.QueueFamilyIndices;
import io.github.lunbun.pulsar.util.vulkan.Vendors;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.stream.Collectors;

/**
 * Handles the selection of a {@link VkPhysicalDevice}.
 * You can request properties of a graphics card, and this will try to match them as closely as possible while still
 * trying to choose the fastest supported graphics card.
 */
public final class PhysicalDevice {
    public final VkPhysicalDevice device;
    public final String name;
    public final String vendor;

    protected PhysicalDevice(VkPhysicalDevice device, String name, String vendor) {
        this.device = device;
        this.name = name;
        this.vendor = vendor;
    }

    public VkPhysicalDeviceLimits getLimits(MemoryStack stack) {
        VkPhysicalDeviceProperties properties = Selector.queryPhysicalDeviceProperties(this.device, stack);
        return properties.limits();
    }

    public static final class Selector {
        public static PhysicalDevice choosePhysicalDevice(Instance instanceManager, WindowSurface surface, GraphicsCardPreference preference) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkPhysicalDevice physicalDevice = null;

                IntBuffer deviceCount = stack.ints(0);
                VK10.vkEnumeratePhysicalDevices(instanceManager.instance, deviceCount, null);
                if (deviceCount.get(0) == 0) {
                    throw new RuntimeException("Failed to find GPUs with Vulkan support");
                }

                PointerBuffer devices = stack.mallocPointer(deviceCount.get(0));
                VK10.vkEnumeratePhysicalDevices(instanceManager.instance, deviceCount, devices);
                int highestScore = 0;
                for (int i = 0; i < devices.capacity(); ++i) {
                    VkPhysicalDevice device = new VkPhysicalDevice(devices.get(i), instanceManager.instance);

                    int score = scoreDevice(device, surface, preference);
                    if (score > highestScore) {
                        physicalDevice = device;
                        highestScore = score;
                    }
                }

                if (physicalDevice == null) {
                    throw new RuntimeException("Failed to find a suitable GPU!");
                }

                VkPhysicalDeviceProperties properties = queryPhysicalDeviceProperties(physicalDevice, stack);
                String name = properties.deviceNameString();
                String vendor = Vendors.getVendorName(properties.vendorID());

                return new PhysicalDevice(physicalDevice, name, vendor);
            }
        }

        private static VkPhysicalDeviceProperties queryPhysicalDeviceProperties(VkPhysicalDevice device, MemoryStack stack) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.mallocStack(stack);
            VK10.vkGetPhysicalDeviceProperties(device, properties);
            return properties;
        }

        private static VkPhysicalDeviceFeatures queryPhysicalDeviceFeatures(VkPhysicalDevice device, MemoryStack stack) {
            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.mallocStack(stack);
            VK10.vkGetPhysicalDeviceFeatures(device, features);
            return features;
        }

        private static boolean checkDeviceExtensionSupport(VkPhysicalDevice device, GraphicsCardPreference preference) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer extensionCount = stack.ints(0);
                VK10.vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, null);

                VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.mallocStack(extensionCount.get(0), stack);
                VK10.vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, availableExtensions);

                return availableExtensions.stream()
                        .map(VkExtensionProperties::extensionNameString)
                        .collect(Collectors.toSet())
                        .containsAll(preference.extensionsSet);
            }
        }

        private static int scoreDevice(VkPhysicalDevice device, WindowSurface surface, GraphicsCardPreference preference) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                QueueFamilyIndices indices = DeviceUtils.findQueueFamilies(device, surface.surface, preference);
                if (!indices.isComplete(preference)) {
                    return -1;
                }

                if (!checkDeviceExtensionSupport(device, preference)) {
                    return -1;
                }

                if (preference.hasSwapChain) {
                    SwapChain.Support.SwapChainSupportDetails swapChainSupport = SwapChain.Support.querySwapChainSupport(device, surface, stack);
                    if ((!swapChainSupport.formats.hasRemaining()) || (!swapChainSupport.presentModes.hasRemaining())) {
                        return -1;
                    }
                }

                if (preference.hasAnisotropicFiltering) {
                    VkPhysicalDeviceFeatures features = queryPhysicalDeviceFeatures(device, stack);
                    if (!features.samplerAnisotropy()) {
                        return -1;
                    }
                }

                int score = 0;

                if (!DeviceType.ANY.equals(preference.type)) {
                    VkPhysicalDeviceProperties properties = queryPhysicalDeviceProperties(device, stack);

                    if (properties.deviceType() == preference.type.id) {
                        score += 1000;
                    }
                } else {
                    score += 1000;
                }

                return score;
            }
        }
    }
}
