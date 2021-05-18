package io.github.lunbun.quasar.mixin.vulkan;

import io.github.lunbun.quasar.client.util.ExtendedBufferVertexConsumer;
import net.minecraft.client.render.BufferVertexConsumer;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferVertexConsumer.class)
public interface MixinBufferVertexConsumer extends ExtendedBufferVertexConsumer {
    @Shadow VertexFormatElement getCurrentElement();

    @Shadow void putFloat(int index, float value);

    @Shadow void nextElement();

    default ExtendedBufferVertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
        ((BufferVertexConsumer) this).vertex(matrix, x, y, z);
        return this;
    }

    default ExtendedBufferVertexConsumer color(int rgba) {
        VertexFormatElement vertexFormatElement = this.getCurrentElement();
        if (vertexFormatElement.getType() != VertexFormatElement.Type.COLOR) {
            return this;
        } else if (vertexFormatElement.getFormat() != VertexFormatElement.Format.UBYTE) {
            throw new IllegalStateException();
        } else {
            this.putFloat(0, Float.intBitsToFloat(rgba));
            this.nextElement();
            return this;
        }
    }

    default void next() {
        ((BufferVertexConsumer) this).next();
    }
}
