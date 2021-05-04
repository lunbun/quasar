package io.github.lunbun.pulsar.component.setup;

import io.github.lunbun.pulsar.component.presentation.SwapChain;
import io.github.lunbun.pulsar.component.presentation.WindowSurface;
import io.github.lunbun.pulsar.struct.setup.DeviceType;
import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.util.DeviceUtils;
import io.github.lunbun.pulsar.util.QueueFamilyIndices;
import io.github.lunbun.pulsar.util.Vendors;
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
public final class PhysicalDeviceManager {
    public VkPhysicalDevice physicalDevice;
    public String name;
    public String vendor;

    public WindowSurface surface;
    public GraphicsCardPreference preference;

    public void pickPhysicalDevice(InstanceManager instanceManager) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.physicalDevice = null;

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

                int score = this.scoreDevice(device);
                if (score > highestScore) {
                    this.physicalDevice = device;
                    highestScore = score;
                }
            }

            if (this.physicalDevice == null) {
                throw new RuntimeException("Failed to find a suitable GPU!");
            }

            this.populateData(this.physicalDevice);
        }
    }

    private void populateData(VkPhysicalDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties properties = queryPhysicalDeviceProperties(device, stack);

            this.name = properties.deviceNameString();
            this.vendor = Vendors.getVendorName(properties.vendorID());
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

    private boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer extensionCount = stack.ints(0);
            VK10.vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.mallocStack(extensionCount.get(0), stack);
            VK10.vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, availableExtensions);

            return availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(Collectors.toSet())
                    .containsAll(this.preference.extensionsSet);
        }
    }

    private int scoreDevice(VkPhysicalDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            QueueFamilyIndices indices = DeviceUtils.findQueueFamilies(device, this.surface.surface, this.preference);
            if (!indices.isComplete(this.preference)) {
                return -1;
            }

            if (!this.checkDeviceExtensionSupport(device)) {
                return -1;
            }

            if (this.preference.hasSwapChain) {
                SwapChain.SwapChainSupportDetails swapChainSupport = SwapChain.querySwapChainSupport(device, this.surface, stack);
                if ((!swapChainSupport.formats.hasRemaining()) || (!swapChainSupport.presentModes.hasRemaining())) {
                    return -1;
                }
            }

            int score = 0;

            if (!DeviceType.ANY.equals(this.preference.type)) {
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
