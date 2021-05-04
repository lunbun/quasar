package io.github.lunbun.pulsar.struct.setup;

import org.lwjgl.vulkan.KHRSwapchain;

public enum DeviceExtension {
    SWAP_CHAIN(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME);

    public final String name;

    DeviceExtension(String name) {
        this.name = name;
    }
}
