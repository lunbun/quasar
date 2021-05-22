package io.github.lunbun.pulsar.component.setup;

import io.github.lunbun.pulsar.component.presentation.WindowSurface;
import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.util.misc.PointerUtils;
import io.github.lunbun.pulsar.util.vulkan.DeviceUtils;
import io.github.lunbun.pulsar.util.vulkan.QueueFamilyIndices;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.Objects;

public final class LogicalDevice {
    public final VkDevice device;

    protected LogicalDevice(VkDevice device) {
        this.device = device;
    }

    public void destroy() {
        VK10.vkDestroyDevice(this.device, null);
    }

    public void waitIdle() {
        VK10.vkDeviceWaitIdle(this.device);
    }

    public static final class Builder {
        public static LogicalDevice createLogicalDevice(PhysicalDevice physicalDevice, WindowSurface surface, GraphicsCardPreference preference, QueueManager queues) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                QueueFamilyIndices indices = DeviceUtils.findQueueFamilies(physicalDevice.device, surface.surface, preference);

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
                deviceFeatures.samplerAnisotropy(preference.hasAnisotropicFiltering);

                VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack);
                createInfo.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);

                createInfo.pQueueCreateInfos(queueCreateInfos);
                createInfo.pEnabledFeatures(deviceFeatures);
                createInfo.ppEnabledExtensionNames(PointerUtils.asPointerBuffer(preference.extensionsSet));

                if (ValidationLayerUtils.ENABLE_VALIDATION_LAYERS) {
                    createInfo.ppEnabledLayerNames(PointerUtils.asPointerBuffer(
                            Objects.requireNonNull(ValidationLayerUtils.VALIDATION_LAYERS)));
                }

                PointerBuffer pDevice = stack.pointers(VK10.VK_NULL_HANDLE);

                if (VK10.vkCreateDevice(physicalDevice.device, createInfo, null, pDevice) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create logical device");
                }

                VkDevice device = new VkDevice(pDevice.get(0), physicalDevice.device, createInfo);

                queues.populate(indices, device);

                return new LogicalDevice(device);
            }
        }
    }
}
