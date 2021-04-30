package io.github.lunbun.quasar.mixin.vulkan;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lunbun.quasar.client.engine.message.MessageBus;
import io.github.lunbun.quasar.client.impl.message.MessageImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

// interfaces the message bus with minecraft's RenderSystem
@Mixin(RenderSystem.class)
public class MixinRenderSystem {
    @Shadow public static void assertThread(Supplier<Boolean> check) {
        throw new UnsupportedOperationException();
    }

    /**
     * @author Lunbun
     * @reason Redirect renderer initializer to initialize Vulkan instead
     */
    @Overwrite
    public static void initRenderer(int debugVerbosity, boolean debugSync) {
        assertThread(RenderSystem::isInInitPhase);
        MessageBus.postMessage(MessageImpl.INIT_VULKAN);
    }
}
