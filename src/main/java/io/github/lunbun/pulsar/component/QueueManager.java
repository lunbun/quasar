package io.github.lunbun.pulsar.component;

import io.github.lunbun.pulsar.struct.QueueFamily;
import io.github.lunbun.pulsar.util.QueueFamilyIndices;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

import java.util.Map;
import java.util.Set;

/**
 * Manages {@link VkQueue} and allows getting them by {@link QueueFamily}
 */
public final class QueueManager {
    private final Map<QueueFamily, VkQueue> queues;

    public QueueManager() {
        this.queues = new Object2ObjectOpenHashMap<>();
    }

    /**
     * Populates the queues using a {@link QueueFamilyIndices} and a {@link VkDevice}
     * @param indices the indices of the queue families
     * @param device the device with the queues
     */
    public void populate(QueueFamilyIndices indices, VkDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.pointers(VK10.VK_NULL_HANDLE);

            // TODO: calling values() on enum allocates memory, make a static value
            for (QueueFamily family : QueueFamily.values()) {
                if (indices.hasFamily(family)) {
                    this.queues.put(family, indices.getQueueFamily(device, family, pQueue));
                }
            }
        }
    }

    /**
     * Gets a {@link VkQueue}
     * @param family the queue family to get
     * @return the {@link VkQueue}
     */
    public VkQueue getQueue(QueueFamily family) {
        return this.queues.get(family);
    }

    /**
     * Checks if there is a queue in a specified family. The queue has to have been requested to exist.
     * @param family the queue family to get
     * @return if there is a queue
     */
    public boolean hasQueue(QueueFamily family) {
        return this.queues.containsKey(family);
    }

    /**
     * Gets the populated queue families
     * @return the queue families that were requested and returned, in a set
     */
    public Set<QueueFamily> getQueueFamilies() {
        return this.queues.keySet();
    }
}
