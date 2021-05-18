package io.github.lunbun.quasar.client.util;

import net.minecraft.util.math.Matrix4f;

public interface ExtendedBufferVertexConsumer {
    ExtendedBufferVertexConsumer vertex(Matrix4f matrix, float x, float y, float z);
    ExtendedBufferVertexConsumer color(int rgba);
    void next();
}
