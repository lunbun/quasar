package io.github.lunbun.pulsar.struct;

import org.lwjgl.vulkan.KHRSwapchain;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class that stores preferred configuration of graphics card.
 */
public class GraphicsCardPreference {
    public final DeviceType type;
    public final List<QueueFamily> queues;
    public final List<DeviceExtension> extensions;

    public final Set<String> extensionsSet;
    public final boolean hasSwapChain;

    public GraphicsCardPreference(DeviceType type, List<QueueFamily> queues, List<DeviceExtension> extensions) {
        this.type = type;
        this.queues = queues;
        this.extensions = extensions;
        this.extensionsSet = extensions.stream()
                .map(extension -> extension.name)
                .collect(Collectors.toSet());
        this.hasSwapChain = extensions.contains(DeviceExtension.SWAP_CHAIN);
    }

    public GraphicsCardPreference(DeviceType type, QueueFamily[] queues, DeviceExtension[] extensions) {
        this(type, Arrays.asList(queues), Arrays.asList(extensions));
    }
}
