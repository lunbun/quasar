package io.github.lunbun.pulsar.component;

import io.github.lunbun.pulsar.struct.GraphicsCardPreference;
import io.github.lunbun.pulsar.util.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.Objects;

/**
 * Handles the creation of {@link VkDevice}.
 */
public final class LogicalDeviceManager {
    public VkDevice device;

    public WindowSurface windowSurface;
    public GraphicsCardPreference preference;

    /**
     * Creates a {@link VkDevice}
     * @param physicalDevice the physical device to use
     * @param queues the {@link QueueManager} to populate
     */
    public void createLogicalDevice(PhysicalDeviceManager physicalDevice, QueueManager queues) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            QueueFamilyIndices indices = DeviceUtils.findQueueFamilies(physicalDevice.physicalDevice,
                    this.windowSurface.surface, this.preference);

            int[] uniqueQueueFamilies = indices.unique();
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos =
                    VkDeviceQueueCreateInfo.callocStack(uniqueQueueFamilies.length, stack);

            float queuePriority = 1;
            for (int i = 0; i < uniqueQueueFamilies.length; ++i) {
                int queueFamily = uniqueQueueFamilies[i];
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(queueFamily);
                queueCreateInfo.pQueuePriorities(stack.floats(queuePriority));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.callocStack(stack);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);

            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pEnabledFeatures(deviceFeatures);
            createInfo.ppEnabledExtensionNames(PointerUtils.asPointerBuffer(this.preference.extensionsSet));

            if (ValidationLayerUtils.ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(PointerUtils.asPointerBuffer(
                        Objects.requireNonNull(ValidationLayerUtils.VALIDATION_LAYERS)));
            }

            PointerBuffer pDevice = stack.pointers(VK10.VK_NULL_HANDLE);

            if (VK10.vkCreateDevice(physicalDevice.physicalDevice, createInfo, null, pDevice) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            this.device = new VkDevice(pDevice.get(0), physicalDevice.physicalDevice, createInfo);

            queues.populate(indices, this.device);
        }
    }

    /**
     * Cleans up the logical device
     */
    public void destroy() {
        VK10.vkDestroyDevice(this.device, null);
    }
}
