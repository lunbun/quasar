package io.github.lunbun.pulsar.util.vulkan;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.vulkan.VK10;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public enum DataType {
    // TODO: do not rely on JOML
    VEC2(VK10.VK_FORMAT_R32G32_SFLOAT, 2 * Float.BYTES, 2 * Float.BYTES, (buffer, object) -> {
        ((Vector2f) object).get(buffer);
        buffer.position(buffer.position() + 2 * Float.BYTES);
    }),
    VEC3(VK10.VK_FORMAT_R32G32B32_SFLOAT, 3 * Float.BYTES, 3 * Float.BYTES, (buffer, object) -> {
        ((Vector3f) object).get(buffer);
        buffer.position(buffer.position() + 3 * Float.BYTES);
    }),
    VEC4(VK10.VK_FORMAT_R32G32B32A32_SFLOAT, 4 * Float.BYTES, 4 * Float.BYTES, (buffer, object) -> {
        ((Vector4f) object).get(buffer);
        buffer.position(buffer.position() + 4 * Float.BYTES);
    }),
    MAT4(VK10.VK_FORMAT_R32G32B32A32_SFLOAT, 16 * Float.BYTES, 4 * Float.BYTES, (buffer, object) -> {
        ((Matrix4f) object).get(buffer);
        buffer.position(buffer.position() + 16 * Float.BYTES);
    });

    public final int format;
    public final int size;
    public final int alignment;
    public final BiConsumer<ByteBuffer, Object> writer;

    DataType(int format, int size, int alignment, BiConsumer<ByteBuffer, Object> writer) {
        this.format = format;
        this.size = size;
        this.alignment = alignment;
        this.writer = writer;
    }
}
