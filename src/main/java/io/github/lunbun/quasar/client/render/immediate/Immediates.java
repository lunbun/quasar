package io.github.lunbun.quasar.client.render.immediate;

import io.github.lunbun.pulsar.PulsarApplication;
import io.github.lunbun.pulsar.component.drawing.CommandBuffer;
import io.github.lunbun.quasar.client.render.VulkanRenderer;

public final class Immediates implements VulkanRenderer {
    @Override
    public void init(PulsarApplication pulsar) {
        for (Immediate immediate : Immediate.VALUES) {
            immediate.init(pulsar);
        }
    }

    @Override
    public void destroy() {
        for (Immediate immediate : Immediate.VALUES) {
            immediate.destroy();
        }
    }

    @Override
    public void destroyFramebuffers() {
        for (Immediate immediate : Immediate.VALUES) {
            immediate.destroyFramebuffers();
        }
    }

    @Override
    public void recreateFramebuffers() {
        for (Immediate immediate : Immediate.VALUES) {
            immediate.recreateFramebuffers();
        }
    }

    @Override
    public void recordCommandBuffers(CommandBuffer buffer, int framebufferIndex, int currentFrame) {
        for (Immediate immediate : Immediate.VALUES) {
            immediate.recordCommandBuffers(buffer, framebufferIndex, currentFrame);
        }
    }
}
