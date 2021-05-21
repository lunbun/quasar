package io.github.lunbun.quasar.mixin.vulkan;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.lunbun.quasar.client.render.immediate.Immediate;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;

// TODO: don't hardcode vertex formats
@Mixin(BufferRenderer.class)
public class MixinBufferRenderer {
    /**
     * @author Lunbun
     * @reason Use vulkan renderer
     */
    @Overwrite
    private static void draw(ByteBuffer buffer, int mode, VertexFormat vertexFormat, int count) {
        buffer.clear();
        if (mode != GL11.GL_TRIANGLES) {
            throw new UnsupportedOperationException("Cannot draw anything except triangles!");
        }

        if (count > 0) {
            if (vertexFormat == VertexFormats.POSITION_COLOR) {
                Immediate.POSITION_COLOR.addVertices(buffer);
                Immediate.POSITION_COLOR.submitMesh();
            } else {
                vertexFormat.startDrawing(MemoryUtil.memAddress(buffer));
                GlStateManager.drawArrays(mode, 0, count);
                vertexFormat.endDrawing();
            }
        }
    }
}
