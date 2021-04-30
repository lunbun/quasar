package io.github.lunbun.pulsar.util;

import io.github.lunbun.pulsar.struct.GraphicsCardPreference;
import io.github.lunbun.pulsar.struct.QueueFamily;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

import java.util.Arrays;
import java.util.Map;

public final class QueueFamilyIndices {
    private final Map<QueueFamily, Integer> families;

    public QueueFamilyIndices() {
        this.families = new Object2IntArrayMap<>();
    }

    public void setFamily(QueueFamily family, Integer index) {
        this.families.put(family, index);
    }

    public boolean hasFamily(QueueFamily family) {
        return this.families.containsKey(family);
    }

    public Integer getFamilyIndex(QueueFamily family) {
        return this.families.get(family);
    }

    public VkQueue getQueueFamily(VkDevice device, QueueFamily family, PointerBuffer pQueue) {
        VK10.vkGetDeviceQueue(device, this.getFamilyIndex(family), 0, pQueue);
        return new VkQueue(pQueue.get(0), device);
    }

    public VkQueue getQueueFamily(VkDevice device, QueueFamily family, MemoryStack stack) {
        return this.getQueueFamily(device, family, stack.pointers(VK10.VK_NULL_HANDLE));
    }

    private boolean isFamilyComplete(GraphicsCardPreference preference, QueueFamily queue) {
        return (!preference.queues.contains(queue)) || (this.families.get(queue) != null);
    }

    // TODO: calling values() on enum allocates memory, make a static value
    public boolean isComplete(GraphicsCardPreference preference) {
        return Arrays.stream(QueueFamily.values())
                .allMatch(family -> isFamilyComplete(preference, family));
    }

    public int[] unique() {
        return families.values().stream()
                .mapToInt(i -> i)
                .distinct()
                .toArray();
    }
}
