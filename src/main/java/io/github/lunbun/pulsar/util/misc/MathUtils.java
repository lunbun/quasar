package io.github.lunbun.pulsar.util.misc;

public class MathUtils {
    public static final int UINT32_MAX = 0xffffffff;
    public static final long UINT64_MAX = 0xffffffffffffffffL;

    public static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }

    public static int ceilIntDivide(int x, int y) {
        return x % y != 0 ? x / y + 1 : x / y;
    }
}
