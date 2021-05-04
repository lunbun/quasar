package io.github.lunbun.pulsar.util;

import io.github.lunbun.pulsar.struct.setup.GraphicsCardPreference;
import io.github.lunbun.pulsar.struct.setup.QueueFamily;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;

public final class DeviceUtils {
    private DeviceUtils() { }

    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device, long surface, GraphicsCardPreference preference) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            QueueFamilyIndices indices = new QueueFamilyIndices();

            IntBuffer queueFamilyCount = stack.ints(0);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies =
                    VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            IntBuffer presentSupport = stack.ints(VK10.VK_FALSE);

            for (int i = 0; i < queueFamilies.capacity(); ++i) {
                VkQueueFamilyProperties queueFamily = queueFamilies.get(i);
                for (QueueFamily queue : preference.queues) {
                    if (QueueFamily.PRESENT.equals(queue)) {
                        KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);

                        if (presentSupport.get(0) == VK10.VK_TRUE) {
                            indices.setFamily(QueueFamily.PRESENT, i);
                        }

                        presentSupport.put(0, VK10.VK_FALSE);
                    } else {
                        if ((queueFamily.queueFlags() & queue.bit) != 0) {
                            indices.setFamily(queue, i);
                        }
                    }
                }

                if (indices.isComplete(preference)) {
                    break;
                }
            }

            return indices;
        }
    }
}
