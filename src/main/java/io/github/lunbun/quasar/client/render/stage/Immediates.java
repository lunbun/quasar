package io.github.lunbun.quasar.client.render.stage;

import io.github.lunbun.pulsar.component.drawing.CommandBuffer;

public final class Immediates {
    private Immediates() { }

    public static void destroy() {
        for (Immediate immediate : Immediate.VALUES) {
            immediate.destroy();
        }
    }

    public static void destroyFramebuffers() {
        for (Immediate immediate : Immediate.VALUES) {
            immediate.destroyFramebuffers();
        }
    }

    public static void recreateFramebuffers() {
        for (Immediate immediate : Immediate.VALUES) {
            immediate.recreateFramebuffers();
        }
    }

    public static void recordCommandBuffers(CommandBuffer buffer, int framebufferIndex, int currentFrame) {
        for (Immediate immediate : Immediate.VALUES) {
            immediate.recordCommandBuffers(buffer, framebufferIndex, currentFrame);
        }
    }
}
