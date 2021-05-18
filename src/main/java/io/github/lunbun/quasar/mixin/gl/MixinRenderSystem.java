package io.github.lunbun.quasar.mixin.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lunbun.quasar.Quasar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

// most of RenderSystem is done with graphics pipelines
@Mixin(RenderSystem.class)
public class MixinRenderSystem {
    /**
     * @author Lunbun
     * @reason Remove OpenGL calls
     */
    @Overwrite
    public static void enableBlend() {
        Quasar.LOGGER.warn("Tried to enable blend!");
    }

    /**
     * @author Lunbun
     * @reason Remove OpenGL calls
     */
    @Overwrite
    public static void disableBlend() {
        Quasar.LOGGER.warn("Tried to disable blend!");
    }

    /**
     * @author Lunbun
     * @reason Remove OpenGL calls
     */
    @Overwrite
    public static void disableTexture() {
        Quasar.LOGGER.warn("Tried to disable texture!");
    }

    /**
     * @author Lunbun
     * @reason Remove OpenGL calls
     */
    @Overwrite
    public static void enableTexture() {
        Quasar.LOGGER.warn("Tried to enable texture!");
    }

    /**
     * @author Lunbun
     * @reason Remove OpenGL calls
     */
    @Overwrite
    public static void blendFuncSeparate(GlStateManager.SrcFactor srcFactor, GlStateManager.DstFactor dstFactor, GlStateManager.SrcFactor srcAlpha, GlStateManager.DstFactor dstAlpha) {
        Quasar.LOGGER.warn("Tried to change blend function!");
    }
}
