package io.github.lunbun.quasar.mixin.gl;

import net.minecraft.client.util.Window;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// remove opengl calls from window
@Mixin(Window.class)
public class MixinWindow {
    @Redirect(at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V"), method = "<init>")
    private void removeMakeContextCurrent(long window) { }

    @Redirect(at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;"), method = "<init>")
    private GLCapabilities removeGlCreateCapabilities() {
        return null;
    }
}
