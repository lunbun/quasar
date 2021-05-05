package io.github.lunbun.pulsar.util.vulkan;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

import java.util.Map;

public final class Vendors {
    private static final Map<Integer, String> VENDOR_NAMES;

    private Vendors() { }

    public static String getVendorName(int vendorID) {
        return VENDOR_NAMES.get(vendorID);
    }

    static {
        VENDOR_NAMES = new Int2ObjectArrayMap<>();

        VENDOR_NAMES.put(0x1002, "AMD");
        VENDOR_NAMES.put(0x1010, "ImgTec");
        VENDOR_NAMES.put(0x10DE, "NVIDIA");
        VENDOR_NAMES.put(0x13B5, "ARM");
        VENDOR_NAMES.put(0x5143, "Qualcomm");
        VENDOR_NAMES.put(0x8086, "Intel");
    }
}
