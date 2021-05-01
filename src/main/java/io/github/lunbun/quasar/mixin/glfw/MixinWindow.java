package io.github.lunbun.quasar.mixin.glfw;

import io.github.lunbun.quasar.client.engine.framework.glfw.GLFWWindow;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(Window.class)
public class MixinWindow {
    private Set<Integer> vanillaHints;

    @Inject(at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", ordinal = 0),
            method = "<init>")
    private void resetVanillaHints(WindowEventHandler eventHandler, MonitorTracker monitorTracker,
                                   WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
        // we need to keep track of which hints vanilla sets so that we don't try to set them twice
        this.vanillaHints = new HashSet<>();
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"), method = "<init>")
    private void glfwWindowHintOverride(int hint, int value) {
        // if our mod requests to override a window hint, override it, if it doesn't use vanilla default hint
        GLFW.glfwWindowHint(hint, GLFWWindow.glfwWindowHints.getOrDefault(hint, value));
        this.vanillaHints.add(hint);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"),
            method = "<init>")
    private void customHints(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings,
                             String videoMode, String title, CallbackInfo ci) {
        // apply custom hints
        GLFWWindow.glfwWindowHints.entrySet().stream()
                .filter(entry -> !this.vanillaHints.contains(entry.getKey()))
                .forEach(hint -> GLFW.glfwWindowHint(hint.getKey(), hint.getValue()));
    }
}
