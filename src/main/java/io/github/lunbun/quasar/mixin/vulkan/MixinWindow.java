package io.github.lunbun.quasar.mixin.vulkan;

import io.github.lunbun.quasar.client.engine.message.MessageBus;
import io.github.lunbun.quasar.client.impl.message.CreateWindowMessage;
import io.github.lunbun.quasar.client.impl.message.MessageImpl;
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

// interfaces the message bus with the window
@Mixin(Window.class)
public class MixinWindow {
    @Inject(at = @At(value = "INVOKE", target =
            "Lcom/mojang/blaze3d/systems/RenderSystem;assertThread(Ljava/util/function/Supplier;)V",
            ordinal = 0), method = "<init>")
    private void initWindow(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings,
                            String videoMode, String title, CallbackInfo ci) {
        MessageBus.postMessage(MessageImpl.INIT_WINDOW);
    }

    @Redirect(at = @At(value = "INVOKE", target =
            "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"), method = "<init>")
    private long createWindow(int width, int height, CharSequence title, long monitor, long share) {
        // intercept the window handle
        long handle = GLFW.glfwCreateWindow(width, height, title, monitor, share);
        MessageBus.postMessage(new CreateWindowMessage(handle));
        return handle;
    }

    @Inject(at = @At(value = "HEAD"), method = "close")
    private void cleanup(CallbackInfo ci) {
        MessageBus.postMessage(MessageImpl.CLEANUP);
    }
}
