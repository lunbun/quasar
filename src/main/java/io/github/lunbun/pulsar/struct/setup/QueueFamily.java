package io.github.lunbun.pulsar.struct.setup;

import org.lwjgl.vulkan.VK10;

public enum QueueFamily {
    GRAPHICS(VK10.VK_QUEUE_GRAPHICS_BIT),
    TRANSFER(VK10.VK_QUEUE_TRANSFER_BIT),
    COMPUTE(VK10.VK_QUEUE_COMPUTE_BIT),
    PRESENT(null); // present needs to be handled differently

    public final Integer bit;

    QueueFamily(Integer bit) {
        this.bit = bit;
    }

    // calling .values() allocates memory since arrays are mutable, but we won't be modifying it so we can use a static
    // variable
    public static final QueueFamily[] VALUES = QueueFamily.values();
}
