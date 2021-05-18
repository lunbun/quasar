package io.github.lunbun.quasar.mixin.vulkan;

import io.github.lunbun.quasar.client.util.ExtendedBufferVertexConsumer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO: index buffers
@Mixin(DrawableHelper.class)
public class MixinDrawableHelper {
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableBlend()V"),
            method = "fill(Lnet/minecraft/util/math/Matrix4f;IIIII)V", cancellable = true)
    private static void overrideFill(Matrix4f matrix, int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
        // its not actually necessary to convert the color into separate channels since it will be converted back in
        // the buffer builder
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        ExtendedBufferVertexConsumer extendedBufferBuilder = (ExtendedBufferVertexConsumer) bufferBuilder;

        bufferBuilder.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        {
            extendedBufferBuilder.vertex(matrix, (float) x1, (float) y1, 0.0F).color(color).next();
            extendedBufferBuilder.vertex(matrix, (float) x2, (float) y1, 0.0F).color(color).next();
            extendedBufferBuilder.vertex(matrix, (float) x2, (float) y2, 0.0F).color(color).next();
            extendedBufferBuilder.vertex(matrix, (float) x1, (float) y1, 0.0F).color(color).next();
            extendedBufferBuilder.vertex(matrix, (float) x1, (float) y2, 0.0F).color(color).next();
            extendedBufferBuilder.vertex(matrix, (float) x2, (float) y2, 0.0F).color(color).next();
        }
        bufferBuilder.end();

        BufferRenderer.draw(bufferBuilder);
        ci.cancel();
    }
}
