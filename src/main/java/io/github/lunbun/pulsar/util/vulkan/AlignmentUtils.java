package io.github.lunbun.pulsar.util.vulkan;

// https://github.com/Naitsirc98/Vulkan-Tutorial-Java/blob/ff0567a6635322d0413196f2ceffe338eef52bdb/src/main/java/javavulkantutorial/AlignmentUtils.java#L41
public final class AlignmentUtils {
    private AlignmentUtils() { }

    public static int alignas(int offset, int alignment) {
        return offset % alignment == 0 ? offset : ((offset - 1) | (alignment - 1)) + 1;
    }
}
