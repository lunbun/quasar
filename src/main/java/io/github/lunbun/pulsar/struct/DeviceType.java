package io.github.lunbun.pulsar.struct;

import org.lwjgl.vulkan.VK10;

public enum DeviceType {
    ANY(null),
    INTEGRATED(VK10.VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU),
    DEDICATED(VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU);

    public final Integer id;

    DeviceType(Integer id) {
        this.id = id;
    }
}
