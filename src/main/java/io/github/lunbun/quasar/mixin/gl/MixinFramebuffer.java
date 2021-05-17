package io.github.lunbun.quasar.mixin.gl;

import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Framebuffer.class)
public class MixinFramebuffer {
    /**
     * @author Lunbun
     * @reason Remove resize
     */
    @Overwrite
    public void resize(int width, int height, boolean getError) { }
}
